package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.*;
import com.teukgeupjeonsa.backend.nutrition.menu.MilitaryMenuNutritionMatch;
import com.teukgeupjeonsa.backend.nutrition.menu.MilitaryMenuNutritionProvider;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FoodData713AnalysisTest {

    private final FoodNameNormalizer normalizer = new FoodNameNormalizer();

    @Test
    void analyzesJuly14LunchWithFoodData713() throws Exception {
        Path xlsx = Path.of("src/main/resources/food_data/food_data_7_13.xlsx");
        Dataset dataset = load(xlsx);

        FoodRepository foodRepository = mock(FoodRepository.class);
        FoodAliasRepository aliasRepository = mock(FoodAliasRepository.class);
        ServingDefaultRepository servingDefaultRepository = mock(ServingDefaultRepository.class);
        stubRepositories(dataset, foodRepository, aliasRepository, servingDefaultRepository);

        FoodMatchOverrideProvider overrideProvider = normalized -> Optional.ofNullable(dataset.overrides().get(normalized));
        ServingEstimator servingEstimator = new ServingEstimator(servingDefaultRepository);
        FoodMatcher matcher = new FoodMatcher(foodRepository, aliasRepository, normalizer, overrideProvider, servingEstimator);
        CompositeFoodEstimator compositeEstimator = new CompositeFoodEstimator(foodRepository, aliasRepository, normalizer, servingDefaultRepository);
        MealNutritionService service = new MealNutritionService(
                normalizer, matcher, servingEstimator, new NutritionCalculator(), compositeEstimator, new MealMenuItemParser());

        List<String> menu = List.of(
                "밥",
                "감자짜글이",
                "쇠고기버섯볶음",
                "훈제연어스테이크&크리미양파드레싱",
                "오이부추무침",
                "초코찰떡",
                "파김치"
        );
        int screenshotCalories = (int) Math.round(360 + 163.52 + 200.48 + 215 + 13.27 + 120 + 10.8);
        NutritionDtos.MealNutritionResponse response = service.analyzeMeal("lunch", menu, screenshotCalories);

        assertThat(response.getItems()).hasSize(7);
        assertThat(response.getMatchedItemCount()).isEqualTo(7);
        assertThat(response.getCalories()).isEqualTo(1083);
        assertThat(response.getItems().stream().map(NutritionDtos.MealNutritionItemResponse::getCalorieKcal)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).sum()).isEqualTo(1083);
        assertThat(response.getItems()).allMatch(item -> Boolean.TRUE.equals(item.getMatched()));
        assertThat(response.getItems().stream().filter(item -> "감자짜글이".equals(item.getMenuName())).findFirst().orElseThrow()
                .getMatchedFoodName()).isEqualTo("감자짜글이");
        assertThat(response.getItems().stream().filter(item -> "초코찰떡".equals(item.getMenuName())).findFirst().orElseThrow()
                .getMatchType()).isEqualTo("OVERRIDE_EXACT");
        System.out.println("FOOD_DATA_7_13_ANALYSIS_BEGIN");
        System.out.printf(Locale.ROOT,
                "TOTAL|official=%d|rawEstimated=%d|display=%d|carb=%.1f|protein=%.1f|fat=%.1f|matched=%d/%d|ratio=%.1f%n",
                response.getOfficialCalorieKcal(), response.getEstimatedCalorieKcal(), response.getCalories(),
                response.getCarbG(), response.getProteinG(), response.getFatG(),
                response.getMatchedItemCount(), response.getTotalItemCount(), response.getMatchedRatio());
        for (NutritionDtos.MealNutritionItemResponse item : response.getItems()) {
            System.out.printf(Locale.ROOT,
                    "ITEM|%s|matched=%s|target=%s|type=%s|confidence=%s|serving=%.1f|kcal=%s|carb=%s|protein=%s|fat=%s|share=%s%n",
                    item.getMenuName(), item.getMatched(), item.getMatchedFoodName(), item.getMatchType(), item.getConfidence(),
                    Optional.ofNullable(item.getServingGram()).orElse(0.0), value(item.getCalorieKcal()),
                    value(item.getCarbohydrateG()), value(item.getProteinG()), value(item.getFatG()), value(item.getCalorieSharePct()));
        }
        System.out.println("FOOD_DATA_7_13_ANALYSIS_END");
    }

    @Test
    void analyzesJuly14DinnerWithFoodData713() throws Exception {
        Path xlsx = Path.of("src/main/resources/food_data/food_data_7_13.xlsx");
        Dataset dataset = load(xlsx);
        MilitaryDataset militaryDataset = loadMilitary(Path.of("src/main/resources/food_data/military_menu_data.xlsx"));

        FoodRepository foodRepository = mock(FoodRepository.class);
        FoodAliasRepository aliasRepository = mock(FoodAliasRepository.class);
        ServingDefaultRepository servingDefaultRepository = mock(ServingDefaultRepository.class);
        stubRepositories(dataset, foodRepository, aliasRepository, servingDefaultRepository);

        FoodMatchOverrideProvider overrideProvider = normalized -> Optional.ofNullable(dataset.overrides().get(normalized));
        ServingEstimator servingEstimator = new ServingEstimator(servingDefaultRepository);
        FoodMatcher matcher = new FoodMatcher(foodRepository, aliasRepository, normalizer, overrideProvider, servingEstimator);
        CompositeFoodEstimator compositeEstimator = new CompositeFoodEstimator(foodRepository, aliasRepository, normalizer, servingDefaultRepository);
        Map<String, Double> exactDinner = Map.of(
                "밥", 360.0, "새우살감자국", 74.85, "분모자마라찜닭", 309.66,
                "해물완자전", 190.0, "들깨무나물", 36.33, "깍두기", 16.0);
        MilitaryMenuNutritionProvider menuProvider = (serviceCode, mealDate, mealType, rawMenu) -> {
            String searchName = normalizer.toSearchName(rawMenu);
            if ("DS_TB_MNDT_DATEBYMLSVC_7296".equals(serviceCode)
                    && LocalDate.of(2026, 7, 14).equals(mealDate) && "dinner".equals(mealType)
                    && exactDinner.containsKey(searchName)) {
                return Optional.of(new MilitaryMenuNutritionMatch(searchName, "군 급식 날짜별 관측",
                        exactDinner.get(searchName), MatchConfidence.HIGH, "DAILY_UNIT_MENU", 1, "7296"));
            }
            return militaryDataset.find(serviceCode, searchName);
        };
        MealNutritionService service = new MealNutritionService(
                normalizer, matcher, servingEstimator, new NutritionCalculator(), compositeEstimator, new MealMenuItemParser(), menuProvider);

        List<String> menu = List.of(
                "밥",
                "새우살감자국",
                "분모자마라찜닭",
                "해물완자전",
                "들깨무나물",
                "깍두기(수의계약)"
        );
        int screenshotCalories = (int) Math.round(360 + 74.85 + 309.66 + 190 + 36.33 + 16);
        NutritionDtos.MealNutritionResponse response = service.analyzeMeal(
                "DS_TB_MNDT_DATEBYMLSVC_7296", LocalDate.of(2026, 7, 14), "dinner", menu, screenshotCalories);

        assertThat(response.getItems()).hasSize(6);
        assertThat(response.getMatchedItemCount()).isEqualTo(6);
        assertThat(response.getItems()).allMatch(item -> Boolean.TRUE.equals(item.getMatched()));
        assertThat(response.getItems()).allMatch(item -> "DAILY_UNIT_MENU".equals(item.getCalorieSource()));
        assertThat(response.getItems()).allMatch(item -> item.getCarbohydrateG() != null
                && item.getProteinG() != null && item.getFatG() != null);
        assertThat(response.getItems().stream().map(NutritionDtos.MealNutritionItemResponse::getCalorieKcal)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).sum()).isEqualTo(987);
        System.out.println("FOOD_DATA_7_14_DINNER_ANALYSIS_BEGIN");
        printResponse(response);
        System.out.println("FOOD_DATA_7_14_DINNER_ANALYSIS_END");
    }

    private void printResponse(NutritionDtos.MealNutritionResponse response) {
        System.out.printf(Locale.ROOT,
                "TOTAL|official=%d|rawEstimated=%d|display=%d|carb=%.1f|protein=%.1f|fat=%.1f|matched=%d/%d|ratio=%.1f%n",
                response.getOfficialCalorieKcal(), response.getEstimatedCalorieKcal(), response.getCalories(),
                response.getCarbG(), response.getProteinG(), response.getFatG(),
                response.getMatchedItemCount(), response.getTotalItemCount(), response.getMatchedRatio());
        for (NutritionDtos.MealNutritionItemResponse item : response.getItems()) {
            System.out.printf(Locale.ROOT,
                    "ITEM|%s|matched=%s|target=%s|type=%s|confidence=%s|serving=%.1f|kcal=%s|carb=%s|protein=%s|fat=%s|share=%s%n",
                    item.getMenuName(), item.getMatched(), item.getMatchedFoodName(), item.getMatchType(), item.getConfidence(),
                    Optional.ofNullable(item.getServingGram()).orElse(0.0), value(item.getCalorieKcal()),
                    value(item.getCarbohydrateG()), value(item.getProteinG()), value(item.getFatG()), value(item.getCalorieSharePct()));
        }
    }

    private String value(Number number) {
        return number == null ? "null" : String.format(Locale.ROOT, "%.1f", number.doubleValue());
    }

    private void stubRepositories(Dataset data, FoodRepository foods, FoodAliasRepository aliases, ServingDefaultRepository defaults) {
        when(foods.findFirstBySearchNameOrderBySourceCountDesc(anyString())).thenAnswer(inv -> bestFood(data.foods(), inv.getArgument(0), true));
        when(foods.findFirstByNameOrderBySourceCountDesc(anyString())).thenAnswer(inv -> bestFood(data.foods(), inv.getArgument(0), false));
        when(foods.searchContains(anyString(), any(Pageable.class))).thenAnswer(inv -> containsFoods(data.foods(), inv.getArgument(0), inv.getArgument(1)));
        when(foods.findByNameContainingIgnoreCaseOrSearchNameContainingIgnoreCase(anyString(), anyString(), any(Pageable.class)))
                .thenAnswer(inv -> containsFoods(data.foods(), inv.getArgument(0), inv.getArgument(2)));

        when(aliases.findFirstBySearchNameOrderByFood_SourceCountDesc(anyString())).thenAnswer(inv -> data.aliases().stream()
                .filter(alias -> alias.getSearchName().equals(inv.getArgument(0)))
                .max(Comparator.comparingInt(alias -> Optional.ofNullable(alias.getFood().getSourceCount()).orElse(0))));
        when(aliases.findBySearchName(anyString())).thenAnswer(inv -> data.aliases().stream()
                .filter(alias -> alias.getSearchName().equals(inv.getArgument(0))).toList());
        when(aliases.searchContains(anyString(), any(Pageable.class))).thenAnswer(inv -> {
            String token = ((String) inv.getArgument(0)).toLowerCase(Locale.ROOT);
            int limit = ((Pageable) inv.getArgument(1)).getPageSize();
            return data.aliases().stream().filter(alias -> aliasText(alias).contains(token)).limit(limit).toList();
        });
        when(defaults.findFirstByCategory(anyString())).thenAnswer(inv -> Optional.ofNullable(data.defaults().get(inv.getArgument(0))));
    }

    private Optional<Food> bestFood(List<Food> foods, String value, boolean searchName) {
        return foods.stream().filter(food -> searchName ? food.getSearchName().equals(value) : food.getName().equals(value))
                .max(Comparator.comparingInt(food -> Optional.ofNullable(food.getSourceCount()).orElse(0)));
    }

    private List<Food> containsFoods(List<Food> foods, String tokenValue, Pageable pageable) {
        String token = normalizer.toSearchName(tokenValue);
        return foods.stream().filter(food -> food.getSearchName().contains(token) || normalizer.toSearchName(food.getName()).contains(token))
                .limit(pageable.getPageSize()).toList();
    }

    private String aliasText(FoodAlias alias) {
        return (alias.getSearchName() + normalizer.toSearchName(alias.getAliasName()) + normalizer.toSearchName(alias.getOriginalName())).toLowerCase(Locale.ROOT);
    }

    private Dataset load(Path path) throws Exception {
        try (InputStream in = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(in)) {
            List<Food> foods = readFoods(workbook.getSheet("food_master"));
            Map<String, Food> byName = new HashMap<>();
            for (Food food : foods) byName.merge(food.getName(), food, (a, b) -> nvl(a.getSourceCount()) >= nvl(b.getSourceCount()) ? a : b);
            List<FoodAlias> aliases = readAliases(workbook.getSheet("food_alias"), byName);
            Map<String, ManualFoodOverride> overrides = readOverrides(workbook.getSheet("manual_overrides"), byName);
            Map<String, ServingDefault> defaults = readDefaults(workbook.getSheet("serving_defaults"));
            return new Dataset(foods, aliases, overrides, defaults);
        }
    }

    private MilitaryDataset loadMilitary(Path path) throws Exception {
        try (InputStream in = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet master = workbook.getSheet("military_menu_master");
            Map<String, Integer> mh = header(master);
            Map<String, MilitaryMenuNutritionMatch> global = new HashMap<>();
            for (int r = 1; r <= master.getLastRowNum(); r++) {
                Row row = master.getRow(r);
                String search = text(row, mh.get("search_name"));
                Double kcal = number(row, mh.get("median_kcal"));
                if (search.isBlank() || kcal == null) continue;
                MatchConfidence confidence;
                try { confidence = MatchConfidence.valueOf(text(row, mh.get("confidence"))); }
                catch (Exception ignored) { confidence = MatchConfidence.NONE; }
                if (confidence == MatchConfidence.NONE) continue;
                global.put(search, new MilitaryMenuNutritionMatch(
                        text(row, mh.get("canonical_name")), text(row, mh.get("category")), kcal, confidence,
                        "GLOBAL_MENU_PROFILE", integer(row, mh.get("valid_kcal_count")), null));
            }

            Sheet units = workbook.getSheet("unit_profiles");
            Map<String, Integer> uh = header(units);
            Map<String, Map<String, MilitaryMenuNutritionMatch>> byUnit = new HashMap<>();
            for (int r = 1; r <= units.getLastRowNum(); r++) {
                Row row = units.getRow(r);
                String search = text(row, uh.get("search_name"));
                String unit = text(row, uh.get("unit_code"));
                Double kcal = number(row, uh.get("median_kcal"));
                MilitaryMenuNutritionMatch base = global.get(search);
                if (base == null || unit.isBlank() || kcal == null) continue;
                MatchConfidence confidence;
                try { confidence = MatchConfidence.valueOf(text(row, uh.get("confidence"))); }
                catch (Exception ignored) { confidence = MatchConfidence.LOW; }
                byUnit.computeIfAbsent(unit, ignored -> new HashMap<>()).put(search,
                        new MilitaryMenuNutritionMatch(base.canonicalName(), base.category(), kcal, confidence,
                                "UNIT_MENU_PROFILE", integer(row, uh.get("valid_kcal_count")), unit));
            }
            return new MilitaryDataset(global, byUnit);
        }
    }

    private List<Food> readFoods(Sheet sheet) {
        Map<String, Integer> h = header(sheet);
        List<Food> result = new ArrayList<>();
        long id = 1;
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            String name = text(row, h.get("food_name"));
            if (name.isBlank()) continue;
            result.add(Food.builder().id(id++).name(name).searchName(normalizer.toSearchName(name))
                    .category(text(row, h.get("display_category"))).servingUnit("100g")
                    .calorie(number(row, h.get("calorie_kcal"))).carbohydrate(number(row, h.get("carbohydrate_g")))
                    .protein(number(row, h.get("protein_g"))).fat(number(row, h.get("fat_g")))
                    .sourceCount(integer(row, h.get("source_count"))).qualityFlag(text(row, h.get("confidence"))).build());
        }
        return result;
    }

    private List<FoodAlias> readAliases(Sheet sheet, Map<String, Food> byName) {
        Map<String, Integer> h = header(sheet);
        List<FoodAlias> result = new ArrayList<>();
        long id = 1;
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            String raw = text(row, h.get("raw_food_name"));
            Food food = byName.get(text(row, h.get("final_food_name")));
            if (raw.isBlank() || food == null) continue;
            result.add(FoodAlias.builder().id(id++).food(food).aliasName(raw).originalName(raw)
                    .searchName(normalizer.toSearchName(raw)).category(text(row, h.get("display_category"))).build());
        }
        return result;
    }

    private Map<String, ManualFoodOverride> readOverrides(Sheet sheet, Map<String, Food> byName) {
        Map<String, Integer> h = header(sheet);
        Map<String, ManualFoodOverride> result = new HashMap<>();
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            String normalized = normalizer.toSearchName(text(row, h.get("normalized_menu_name")));
            Food food = byName.get(text(row, h.get("matched_food_name")));
            if (normalized.isBlank() || food == null) continue;
            result.put(normalized, ManualFoodOverride.builder().normalizedMenuName(normalized).food(food)
                    .confidence(MatchConfidence.valueOf(text(row, h.get("confidence"))))
                    .defaultServingGram(number(row, h.get("serving_g"))).build());
        }
        return result;
    }

    private Map<String, ServingDefault> readDefaults(Sheet sheet) {
        Map<String, Integer> h = header(sheet);
        Map<String, ServingDefault> result = new HashMap<>();
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            String category = text(row, h.get("display_category"));
            Double gram = number(row, h.get("default_serving_g"));
            if (!category.isBlank() && gram != null) result.put(category, ServingDefault.builder().category(category).servingGram(gram).build());
        }
        return result;
    }

    private Map<String, Integer> header(Sheet sheet) {
        Map<String, Integer> result = new HashMap<>();
        for (Cell cell : sheet.getRow(0)) result.put(cell.getStringCellValue(), cell.getColumnIndex());
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

    private int nvl(Integer value) { return value == null ? 0 : value; }

    private record Dataset(List<Food> foods, List<FoodAlias> aliases,
                           Map<String, ManualFoodOverride> overrides, Map<String, ServingDefault> defaults) { }

    private record MilitaryDataset(Map<String, MilitaryMenuNutritionMatch> global,
                                   Map<String, Map<String, MilitaryMenuNutritionMatch>> byUnit) {
        Optional<MilitaryMenuNutritionMatch> find(String serviceCode, String searchName) {
            String unit = Optional.ofNullable(serviceCode).orElse("").replaceAll(".*?(\\d+)$", "$1");
            MilitaryMenuNutritionMatch unitMatch = byUnit.getOrDefault(unit, Map.of()).get(searchName);
            return Optional.ofNullable(unitMatch != null ? unitMatch : global.get(searchName));
        }
    }
}
