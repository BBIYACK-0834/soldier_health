package com.teukgeupjeonsa.backend.nutrition.menu;

import com.teukgeupjeonsa.backend.nutrition.MatchConfidence;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MilitaryMenuXlsxImporter {
    private final MilitaryMenuProfileRepository profileRepository;
    private final MilitaryMenuUnitProfileRepository unitProfileRepository;

    @Transactional
    public MilitaryMenuImportResult importXlsx(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("군 급식 메뉴 xlsx 파일을 찾을 수 없습니다: " + path);
        }
        try (InputStream in = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet master = requiredSheet(workbook, "military_menu_master");
            Sheet units = requiredSheet(workbook, "unit_profiles");
            ReadProfiles profiles = readProfiles(master);
            validateSource(master, units, profiles);

            unitProfileRepository.deleteAllInBatch();
            profileRepository.deleteAllInBatch();
            List<MilitaryMenuProfile> saved = profileRepository.saveAll(profiles.values());
            Map<String, MilitaryMenuProfile> bySearchName = new HashMap<>();
            for (MilitaryMenuProfile profile : saved) bySearchName.put(profile.getSearchName(), profile);

            ReadUnitProfiles unitProfiles = readUnitProfiles(units, bySearchName);
            unitProfileRepository.saveAll(unitProfiles.values());
            return new MilitaryMenuImportResult(saved.size(), unitProfiles.values().size(), 0,
                    profiles.skipped(), unitProfiles.skipped());
        } catch (Exception e) {
            throw new IllegalStateException("군 급식 메뉴 xlsx import 실패: " + path, e);
        }
    }

    private void validateSource(Sheet master, Sheet units, ReadProfiles profiles) {
        requireHeaders(master, "canonical_name", "search_name", "median_kcal", "confidence");
        requireHeaders(units, "canonical_name", "search_name", "unit_code", "median_kcal", "confidence");
        if (profiles.values().size() < 100) {
            throw new IllegalArgumentException("군 급식 표준 메뉴가 너무 적습니다: " + profiles.values().size());
        }
        int usableUnitRows = 0;
        Map<String,Integer> h = headers(units);
        for (int r = 1; r <= units.getLastRowNum(); r++) {
            Row row = units.getRow(r);
            if (!text(row, h.get("search_name")).isBlank()
                    && !text(row, h.get("unit_code")).isBlank()
                    && number(row, h.get("median_kcal")) != null) usableUnitRows++;
        }
        if (usableUnitRows < 100) {
            throw new IllegalArgumentException("유효한 부대별 메뉴 프로필이 너무 적습니다: " + usableUnitRows);
        }
    }

    private void requireHeaders(Sheet sheet, String... names) {
        Map<String,Integer> actual = headers(sheet);
        List<String> missing = Arrays.stream(names).filter(name -> !actual.containsKey(name)).toList();
        if (!missing.isEmpty()) throw new IllegalArgumentException(sheet.getSheetName() + " 필수 컬럼 누락: " + missing);
    }

    private ReadProfiles readProfiles(Sheet sheet) {
        Map<String,Integer> h = headers(sheet);
        Map<String,MilitaryMenuProfile> profiles = new LinkedHashMap<>();
        int skipped = 0;
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            String canonical = text(row, h.get("canonical_name"));
            String search = text(row, h.get("search_name"));
            Double median = number(row, h.get("median_kcal"));
            MatchConfidence confidence = confidence(text(row, h.get("confidence")));
            if (!isSaneMenu(canonical, search) || median == null || confidence == MatchConfidence.NONE) { skipped++; continue; }
            profiles.putIfAbsent(search, MilitaryMenuProfile.builder()
                    .canonicalName(canonical).searchName(search).category(text(row, h.get("category")))
                    .medianKcal(median).meanKcal(number(row, h.get("mean_kcal"))).q1Kcal(number(row, h.get("q1_kcal")))
                    .q3Kcal(number(row, h.get("q3_kcal"))).minKcal(number(row, h.get("min_kcal"))).maxKcal(number(row, h.get("max_kcal")))
                    .validKcalCount(integer(row, h.get("valid_kcal_count"))).observationCount(integer(row, h.get("observation_count")))
                    .unitCount(integer(row, h.get("unit_count"))).firstDate(date(row, h.get("first_date"))).lastDate(date(row, h.get("last_date")))
                    .confidence(confidence).reviewReason(text(row, h.get("review_reason"))).build());
        }
        return new ReadProfiles(new ArrayList<>(profiles.values()), skipped);
    }

    private ReadUnitProfiles readUnitProfiles(Sheet sheet, Map<String,MilitaryMenuProfile> menus) {
        Map<String,Integer> h = headers(sheet);
        Map<String,MilitaryMenuUnitProfile> profiles = new LinkedHashMap<>();
        int skipped = 0;
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            String search = text(row, h.get("search_name"));
            String unit = text(row, h.get("unit_code"));
            Double median = number(row, h.get("median_kcal"));
            MilitaryMenuProfile menu = menus.get(search);
            if (menu == null || unit.isBlank() || median == null) { skipped++; continue; }
            String key = search + "\n" + unit;
            profiles.putIfAbsent(key, MilitaryMenuUnitProfile.builder()
                    .menu(menu).unitCode(unit).medianKcal(median).q1Kcal(number(row, h.get("q1_kcal"))).q3Kcal(number(row, h.get("q3_kcal")))
                    .validKcalCount(integer(row, h.get("valid_kcal_count"))).observationCount(integer(row, h.get("observation_count")))
                    .firstDate(date(row, h.get("first_date"))).lastDate(date(row, h.get("last_date")))
                    .confidence(confidence(text(row, h.get("confidence")))).build());
        }
        return new ReadUnitProfiles(new ArrayList<>(profiles.values()), skipped);
    }

    private boolean isSaneMenu(String canonical, String search) {
        if (canonical.isBlank() || search.isBlank() || canonical.length() > 240 || search.length() > 240) return false;
        if (!canonical.matches(".*[A-Za-z가-힣].*")) return false;
        return !canonical.matches("(?i)^\\d+(?:\\.\\d+)?\\s*kcal$");
    }

    private Sheet requiredSheet(Workbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        if (sheet == null) throw new IllegalArgumentException("필수 시트가 없습니다: " + name);
        return sheet;
    }

    private Map<String,Integer> headers(Sheet sheet) {
        if (sheet.getRow(0) == null) throw new IllegalArgumentException(sheet.getSheetName() + " 헤더 행이 없습니다.");
        Map<String,Integer> result = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : sheet.getRow(0)) {
            String name = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
            if (!name.isBlank()) result.put(name, cell.getColumnIndex());
        }
        return result;
    }

    private String text(Row row, Integer column) {
        if (row == null || column == null) return "";
        Cell cell = row.getCell(column);
        return cell == null ? "" : new DataFormatter().formatCellValue(cell).trim();
    }

    private Double number(Row row, Integer column) {
        String value = text(row, column);
        if (value.isBlank()) return null;
        try { return Double.parseDouble(value.replace(",", "")); } catch (NumberFormatException ignored) { return null; }
    }

    private Integer integer(Row row, Integer column) {
        Double value = number(row, column);
        return value == null ? 0 : value.intValue();
    }

    private LocalDate date(Row row, Integer column) {
        String value = text(row, column);
        try { return value.isBlank() ? null : LocalDate.parse(value); } catch (Exception ignored) { return null; }
    }

    private MatchConfidence confidence(String value) {
        try { return MatchConfidence.valueOf(value.toUpperCase(Locale.ROOT)); } catch (Exception ignored) { return MatchConfidence.NONE; }
    }

    private record ReadProfiles(List<MilitaryMenuProfile> values, int skipped) { }
    private record ReadUnitProfiles(List<MilitaryMenuUnitProfile> values, int skipped) { }
}
