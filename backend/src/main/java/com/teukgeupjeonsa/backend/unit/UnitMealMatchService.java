package com.teukgeupjeonsa.backend.unit;

import com.teukgeupjeonsa.backend.meal.entity.MealMenu;
import com.teukgeupjeonsa.backend.meal.repository.MealMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitMealMatchService {

    private static final double BREAKFAST_WEIGHT = 0.25;
    private static final double LUNCH_WEIGHT = 0.40;
    private static final double DINNER_WEIGHT = 0.35;
    private static final int MAX_RESULTS = 5;

    private static final Pattern ALLERGY_CODE_PATTERN = Pattern.compile("\\(\\s*\\d{1,2}\\s*\\)");
    private static final Pattern NON_KOREAN_OR_ALNUM_PATTERN = Pattern.compile("[^가-힣a-z0-9\\s]");

    private static final Map<String, String> TOKEN_SYNONYMS = Map.ofEntries(
            Map.entry("배추김치", "김치"),
            Map.entry("포기김치", "김치"),
            Map.entry("열무김치", "김치"),
            Map.entry("백김치", "김치"),
            Map.entry("백미밥", "쌀밥"),
            Map.entry("흰쌀밥", "쌀밥"),
            Map.entry("요구르트", "발효유"),
            Map.entry("요거트", "발효유"),
            Map.entry("요플레", "발효유")
    );

    private final MealMenuRepository mealMenuRepository;
    private final MilitaryUnitRepository militaryUnitRepository;

    @Transactional(readOnly = true)
    public MealMenuCandidatesResponse getMealMenuCandidates(LocalDate date, String mealTypeRaw) {
        MealType mealType = MealType.from(mealTypeRaw);

        List<MealMenu> menus = mealMenuRepository.findByMealDate(date);
        if (menus.isEmpty()) {
            return MealMenuCandidatesResponse.builder()
                    .mealType(mealType.apiValue())
                    .date(date)
                    .candidateCount(0)
                    .menus(List.of())
                    .build();
        }

        Map<String, Long> menuFreq = new HashMap<>();
        for (MealMenu mealMenu : menus) {
            parseMealMenus(extractMealText(mealMenu, mealType)).stream()
                    .forEach(menu -> menuFreq.merge(menu, 1L, Long::sum));
        }

        List<String> candidates = menuFreq.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(80)
                .map(Map.Entry::getKey)
                .toList();

        return MealMenuCandidatesResponse.builder()
                .mealType(mealType.apiValue())
                .date(date)
                .candidateCount(candidates.size())
                .menus(candidates)
                .build();
    }

    @Transactional(readOnly = true)
    public MatchUnitsBySelectedMenusResponse matchBySelectedMenus(MatchUnitsBySelectedMenusRequest request) {
        MealType mealType = MealType.from(request.getMealType());
        Set<String> selected = request.getSelectedMenus().stream()
                .map(this::normalizeNullable)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (selected.isEmpty()) {
            return MatchUnitsBySelectedMenusResponse.builder()
                    .candidateCount(0)
                    .units(List.of())
                    .build();
        }

        List<MealMenu> menus = mealMenuRepository.findByMealDate(request.getDate());
        if (menus.isEmpty()) {
            return MatchUnitsBySelectedMenusResponse.builder()
                    .candidateCount(0)
                    .units(List.of())
                    .build();
        }

        Map<String, MilitaryUnit> unitsByServiceCode = militaryUnitRepository.findByDataSourceKeyIn(
                        menus.stream()
                                .map(MealMenu::getServiceCode)
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(code -> !code.isBlank())
                                .distinct()
                                .toList()
                ).stream()
                .filter(unit -> unit.getDataSourceKey() != null && !unit.getDataSourceKey().isBlank())
                .collect(Collectors.toMap(unit -> unit.getDataSourceKey().trim(), unit -> unit, (a, b) -> a));

        List<MatchedMenuCandidate> candidates = menus.stream()
                .map(menu -> toMatchedMenuCandidate(menu, mealType, selected, unitsByServiceCode))
                .filter(Objects::nonNull)
                .filter(candidate -> candidate.matchCount > 0)
                .sorted(Comparator
                        .comparingInt(MatchedMenuCandidate::matchCount).reversed()
                        .thenComparingDouble(MatchedMenuCandidate::matchScore).reversed()
                        .thenComparing(candidate -> candidate.unit.getUnitName()))
                .toList();

        return MatchUnitsBySelectedMenusResponse.builder()
                .candidateCount(candidates.size())
                .units(candidates.stream().map(MatchedMenuCandidate::toResponse).toList())
                .build();
    }

    private MatchedMenuCandidate toMatchedMenuCandidate(
            MealMenu mealMenu,
            MealType mealType,
            Set<String> selectedMenus,
            Map<String, MilitaryUnit> unitsByServiceCode
    ) {
        String serviceCode = normalizeNullable(mealMenu.getServiceCode());
        if (serviceCode == null) {
            return null;
        }

        MilitaryUnit unit = unitsByServiceCode.get(serviceCode);
        if (unit == null) {
            return null;
        }

        Set<String> mealMenus = parseMealMenus(extractMealText(mealMenu, mealType));
        if (mealMenus.isEmpty()) {
            return null;
        }

        int matchCount = (int) selectedMenus.stream().filter(mealMenus::contains).count();
        double matchScore = selectedMenus.isEmpty() ? 0.0 : (double) matchCount / selectedMenus.size();

        return new MatchedMenuCandidate(unit, matchCount, matchScore, extractMealText(mealMenu, mealType));
    }

    private Set<String> parseMealMenus(String rawMealText) {
        if (rawMealText == null || rawMealText.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(rawMealText.split(","))
                .map(this::normalizeNullable)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String extractMealText(MealMenu mealMenu, MealType mealType) {
        return switch (mealType) {
            case BREAKFAST -> mealMenu.getBreakfast();
            case LUNCH -> mealMenu.getLunch();
            case DINNER -> mealMenu.getDinner();
        };
    }

    @Transactional(readOnly = true)
    public List<MatchedUnitResponse> matchByMeal(MatchUnitByMealRequest request) {
        String inputBreakfast = normalizeNullable(request.getBreakfast());
        String inputLunch = normalizeNullable(request.getLunch());
        String inputDinner = normalizeNullable(request.getDinner());

        if (inputBreakfast == null && inputLunch == null && inputDinner == null) {
            throw new IllegalArgumentException("아침/점심/저녁 중 하나 이상 입력해주세요.");
        }

        LocalDate date = request.getDate();
        List<MealMenu> menus = mealMenuRepository.findByMealDate(date);
        if (menus.isEmpty()) {
            return List.of();
        }

        Map<String, MilitaryUnit> unitByServiceCode = militaryUnitRepository.findByDataSourceKeyIn(
                        menus.stream()
                                .map(MealMenu::getServiceCode)
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(code -> !code.isBlank())
                                .distinct()
                                .toList()
                ).stream()
                .filter(unit -> unit.getDataSourceKey() != null && !unit.getDataSourceKey().isBlank())
                .collect(Collectors.toMap(unit -> unit.getDataSourceKey().trim(), unit -> unit, (a, b) -> a));

        return menus.stream()
                .map(menu -> scoreMenu(menu, unitByServiceCode, inputBreakfast, inputLunch, inputDinner))
                .filter(Objects::nonNull)
                .filter(candidate -> candidate.totalScore > 0.0)
                .sorted(Comparator
                        .comparingDouble(MatchCandidate::totalScore).reversed()
                        .thenComparingInt(MatchCandidate::matchedMealCount).reversed()
                        .thenComparing(candidate -> candidate.unit.getUnitName()))
                .limit(MAX_RESULTS)
                .map(MatchCandidate::toResponse)
                .toList();
    }

    private MatchCandidate scoreMenu(
            MealMenu menu,
            Map<String, MilitaryUnit> unitByServiceCode,
            String inputBreakfast,
            String inputLunch,
            String inputDinner
    ) {
        String serviceCode = normalizeNullable(menu.getServiceCode());
        if (serviceCode == null) {
            return null;
        }

        MilitaryUnit unit = unitByServiceCode.get(serviceCode);
        if (unit == null) {
            return null;
        }

        double breakfastScore = scoreMeal(inputBreakfast, menu.getBreakfast());
        double lunchScore = scoreMeal(inputLunch, menu.getLunch());
        double dinnerScore = scoreMeal(inputDinner, menu.getDinner());

        double totalScore = breakfastScore * BREAKFAST_WEIGHT
                + lunchScore * LUNCH_WEIGHT
                + dinnerScore * DINNER_WEIGHT;

        List<String> matchedMeals = new ArrayList<>();
        if (breakfastScore > 0) {
            matchedMeals.add("아침");
        }
        if (lunchScore > 0) {
            matchedMeals.add("점심");
        }
        if (dinnerScore > 0) {
            matchedMeals.add("저녁");
        }

        return new MatchCandidate(unit, totalScore, matchedMeals, breakfastScore, lunchScore, dinnerScore, menu);
    }

    private double scoreMeal(String userInput, String mealMenuText) {
        Set<String> userTokens = tokenize(userInput);
        Set<String> menuTokens = tokenize(mealMenuText);

        if (userTokens.isEmpty() || menuTokens.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(userTokens);
        intersection.retainAll(menuTokens);

        Set<String> union = new HashSet<>(userTokens);
        union.addAll(menuTokens);
        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String source) {
        String normalized = normalizeNullable(source);
        if (normalized == null) {
            return Set.of();
        }

        String tokenSeparated = normalized
                .replace(',', ' ')
                .replace('/', ' ')
                .replace('·', ' ')
                .replace('|', ' ');

        return Arrays.stream(tokenSeparated.split("\\s+"))
                .map(this::canonicalizeToken)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String canonicalizeToken(String rawToken) {
        String token = rawToken == null ? "" : rawToken.trim();
        if (token.isBlank()) {
            return "";
        }
        return TOKEN_SYNONYMS.getOrDefault(token, token);
    }

    private String normalizeNullable(String raw) {
        if (raw == null) {
            return null;
        }

        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim();
        normalized = ALLERGY_CODE_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = NON_KOREAN_OR_ALNUM_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized.isBlank() ? null : normalized;
    }

    private record MatchCandidate(
            MilitaryUnit unit,
            double totalScore,
            List<String> matchedMeals,
            double breakfastScore,
            double lunchScore,
            double dinnerScore,
            MealMenu menu
    ) {
        int matchedMealCount() {
            return matchedMeals.size();
        }

        MatchedUnitResponse toResponse() {
            return MatchedUnitResponse.builder()
                    .unitId(unit.getId())
                    .unitName(unit.getUnitName())
                    .branchType(unit.getBranchType().name())
                    .regionName(unit.getRegionName())
                    .matchScore(Math.round(totalScore * 1000.0) / 1000.0)
                    .matchedMeals(matchedMeals)
                    .mealPreview(MatchedUnitResponse.MealPreview.builder()
                            .breakfast(menu.getBreakfast())
                            .lunch(menu.getLunch())
                            .dinner(menu.getDinner())
                            .build())
                    .mealMatchDetail(MatchedUnitResponse.MealMatchDetail.builder()
                            .breakfastScore(Math.round(breakfastScore * 1000.0) / 1000.0)
                            .lunchScore(Math.round(lunchScore * 1000.0) / 1000.0)
                            .dinnerScore(Math.round(dinnerScore * 1000.0) / 1000.0)
                            .build())
                    .build();
        }
    }

    private record MatchedMenuCandidate(
            MilitaryUnit unit,
            int matchCount,
            double matchScore,
            String mealPreview
    ) {
        MatchUnitsBySelectedMenusResponse.UnitMealMatchCard toResponse() {
            return MatchUnitsBySelectedMenusResponse.UnitMealMatchCard.builder()
                    .unitId(unit.getId())
                    .unitName(unit.getUnitName())
                    .branchType(unit.getBranchType().name())
                    .regionName(unit.getRegionName())
                    .matchCount(matchCount)
                    .matchScore(Math.round(matchScore * 1000.0) / 1000.0)
                    .mealPreview(mealPreview)
                    .build();
        }
    }
}
