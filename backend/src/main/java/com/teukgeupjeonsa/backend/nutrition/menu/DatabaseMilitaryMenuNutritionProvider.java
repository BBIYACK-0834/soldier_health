package com.teukgeupjeonsa.backend.nutrition.menu;

import com.teukgeupjeonsa.backend.nutrition.FoodNameNormalizer;
import com.teukgeupjeonsa.backend.nutrition.MatchConfidence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DatabaseMilitaryMenuNutritionProvider implements MilitaryMenuNutritionProvider {
    private static final Pattern TRAILING_UNIT_CODE = Pattern.compile("(\\d+)$");

    private final MilitaryMenuProfileRepository profileRepository;
    private final MilitaryMenuUnitProfileRepository unitProfileRepository;
    private final MilitaryMenuDailyProfileRepository dailyProfileRepository;
    private final FoodNameNormalizer normalizer;

    @Override
    @Transactional(readOnly = true)
    public Optional<MilitaryMenuNutritionMatch> find(String serviceCode, LocalDate mealDate, String mealType, String rawMenuName) {
        String searchName = normalizer.toSearchName(rawMenuName);
        if (searchName.isBlank()) return Optional.empty();
        String unitCode = extractUnitCode(serviceCode);
        if (unitCode != null && mealDate != null && mealType != null && !mealType.isBlank()) {
            Optional<MilitaryMenuDailyProfile> daily = findDailyProfile(
                    unitCode, mealDate, mealType, searchName);
            if (daily.isPresent()) {
                MilitaryMenuDailyProfile value = daily.get();
                return Optional.of(new MilitaryMenuNutritionMatch(
                        value.getCanonicalName(), "군 급식 날짜별 관측", value.getCalorieKcal(),
                        MatchConfidence.HIGH, "DAILY_UNIT_MENU", value.getSampleCount(), unitCode));
            }
        }
        Optional<MilitaryMenuProfile> profileOptional = profileRepository.findFirstBySearchName(searchName);
        if (profileOptional.isEmpty()) return Optional.empty();
        MilitaryMenuProfile profile = profileOptional.get();

        if (unitCode != null) {
            Optional<MilitaryMenuUnitProfile> unitProfile = unitProfileRepository.findFirstByMenuAndUnitCode(profile, unitCode);
            if (unitProfile.isPresent() && unitProfile.get().getMedianKcal() != null) {
                MilitaryMenuUnitProfile value = unitProfile.get();
                return Optional.of(new MilitaryMenuNutritionMatch(
                        profile.getCanonicalName(), profile.getCategory(), value.getMedianKcal(),
                        Optional.ofNullable(value.getConfidence()).orElse(MatchConfidence.LOW),
                        "UNIT_MENU_PROFILE", Optional.ofNullable(value.getValidKcalCount()).orElse(0), unitCode));
            }
        }

        if (profile.getMedianKcal() == null || profile.getConfidence() == MatchConfidence.NONE) return Optional.empty();
        return Optional.of(new MilitaryMenuNutritionMatch(
                profile.getCanonicalName(), profile.getCategory(), profile.getMedianKcal(),
                Optional.ofNullable(profile.getConfidence()).orElse(MatchConfidence.LOW),
                "GLOBAL_MENU_PROFILE", Optional.ofNullable(profile.getValidKcalCount()).orElse(0), null));
    }

    private Optional<MilitaryMenuDailyProfile> findDailyProfile(
            String unitCode, LocalDate mealDate, String mealType, String searchName) {
        Optional<MilitaryMenuDailyProfile> exact = dailyProfileRepository
                .findFirstByUnitCodeAndMealDateAndMealTypeAndSearchName(
                        unitCode, mealDate, mealType, searchName);
        if (exact.isPresent()) return exact;

        // 원본 API는 "우유(백색우유(200ML,연간))"를 우유백색우유로 저장하지만
        // 화면 메뉴 파서는 백색우유로 정규화하므로 날짜별 공식 칼로리를 한 번 더 찾는다.
        if ("백색우유".equals(searchName)) {
            return dailyProfileRepository.findFirstByUnitCodeAndMealDateAndMealTypeAndSearchName(
                    unitCode, mealDate, mealType, "우유백색우유");
        }
        return Optional.empty();
    }

    private String extractUnitCode(String serviceCode) {
        if (serviceCode == null || serviceCode.isBlank()) return null;
        Matcher matcher = TRAILING_UNIT_CODE.matcher(serviceCode.trim());
        return matcher.find() ? matcher.group(1) : null;
    }
}
