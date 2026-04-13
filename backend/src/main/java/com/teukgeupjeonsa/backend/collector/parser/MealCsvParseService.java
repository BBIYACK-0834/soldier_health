package com.teukgeupjeonsa.backend.collector.parser;

import com.teukgeupjeonsa.backend.collector.dto.ParsedMealRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealCsvParseService {

    private final CsvColumnAliasResolver aliasResolver;

    public List<ParsedMealRow> parse(Path csvPath, String sourceTitle) {
        for (Charset charset : List.of(StandardCharsets.UTF_8, Charset.forName("EUC-KR"), Charset.forName("MS949"))) {
            try (Reader reader = Files.newBufferedReader(csvPath, charset);
                 CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build().parse(reader)) {
                return parseRecords(parser, sourceTitle);
            } catch (Exception e) {
                log.warn("CSV 파싱 재시도 charset={}, file={}", charset, csvPath);
            }
        }
        throw new IllegalArgumentException("CSV 인코딩/형식 파싱 실패: " + csvPath);
    }

    private List<ParsedMealRow> parseRecords(CSVParser parser, String sourceTitle) throws IOException {
        String unitCol = aliasResolver.resolve(parser.getHeaderMap(), "unit");
        String dateCol = aliasResolver.resolve(parser.getHeaderMap(), "date");
        String breakfastCol = aliasResolver.resolve(parser.getHeaderMap(), "breakfast");
        String lunchCol = aliasResolver.resolve(parser.getHeaderMap(), "lunch");
        String dinnerCol = aliasResolver.resolve(parser.getHeaderMap(), "dinner");

        if (dateCol == null) {
            throw new IllegalArgumentException("날짜 컬럼 매핑 실패");
        }

        String titleUnit = extractUnitFromTitle(sourceTitle);
        List<ParsedMealRow> rows = new ArrayList<>();
        for (CSVRecord rec : parser.getRecords()) {
            LocalDate date = parseDate(rec.get(dateCol));
            if (date == null) {
                continue;
            }
            String unitName = clean(unitCol == null ? titleUnit : rec.get(unitCol));
            if (unitName == null || unitName.isBlank()) unitName = titleUnit;
            String breakfast = value(rec, breakfastCol);
            String lunch = value(rec, lunchCol);
            String dinner = value(rec, dinnerCol);
            String raw = String.join("|", unitName, date.toString(), nullSafe(breakfast), nullSafe(lunch), nullSafe(dinner));
            rows.add(ParsedMealRow.builder()
                    .unitName(unitName)
                    .branch(inferBranch(unitName))
                    .mealDate(date)
                    .breakfast(breakfast)
                    .lunch(lunch)
                    .dinner(dinner)
                    .rawRowHash(sha256(raw))
                    .build());
        }
        return rows;
    }

    private String value(CSVRecord record, String col) {
        return col == null ? null : clean(record.get(col));
    }

    private String extractUnitFromTitle(String title) {
        if (title == null) return "미상부대";
        return title.replaceFirst("^국방부_", "").replaceFirst("\\s*식단정보$", "").trim();
    }

    private String inferBranch(String unitName) {
        if (unitName == null) return null;
        if (unitName.contains("육")) return "육군";
        if (unitName.contains("해")) return "해군";
        if (unitName.contains("공")) return "공군";
        if (unitName.contains("해병")) return "해병대";
        return null;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null) return null;
        String normalized = raw.replaceAll("[^0-9-]", "");
        try {
            if (normalized.matches("\\d{8}")) return LocalDate.parse(normalized, DateTimeFormatter.BASIC_ISO_DATE);
            if (normalized.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(normalized);
        } catch (Exception ignored) {
        }
        return null;
    }

    private String clean(String raw) {
        if (raw == null) return null;
        String v = raw.replace("\r", "").trim();
        return v.isBlank() ? null : v;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
