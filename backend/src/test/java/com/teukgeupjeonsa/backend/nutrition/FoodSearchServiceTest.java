package com.teukgeupjeonsa.backend.nutrition;

import com.teukgeupjeonsa.backend.food.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FoodSearchServiceTest {

    private final FoodRepository foodRepository = mock(FoodRepository.class);
    private final FoodAliasRepository aliasRepository = mock(FoodAliasRepository.class);
    private final FoodNameNormalizer normalizer = new FoodNameNormalizer();
    private final FoodMatchOverrideProvider overrideProvider = mock(FoodMatchOverrideProvider.class);
    private final ServingDefaultRepository servingDefaultRepository = mock(ServingDefaultRepository.class);
    private final ServingEstimator servingEstimator = new ServingEstimator(servingDefaultRepository);
    private final FoodSearchService service = new FoodSearchService(foodRepository, aliasRepository, normalizer, overrideProvider, servingEstimator);

    @Test
    void riceSearchDoesNotReturnDessert() {
        Food rice = food(1L, "백미밥", "밥류", "백미밥", 150.0, 3);
        Food dessert = food(2L, "밥아이스크림", "디저트류", "밥아이스크림", 250.0, 9);
        when(overrideProvider.findOverride("밥")).thenReturn(Optional.empty());
        when(aliasRepository.findBySearchName("밥")).thenReturn(List.of(alias(rice, "밥"), alias(dessert, "밥")));
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc("밥")).thenReturn(Optional.empty());
        when(aliasRepository.countByFood_Id(1L)).thenReturn(2L);

        NutritionDtos.FoodSearchResponse response = service.search("밥");

        assertThat(response.getResults()).extracting(NutritionDtos.FoodSearchItemResponse::getRepresentativeName)
                .containsExactly("백미밥");
    }

    @Test
    void searchResultsAreGroupedByRepresentativeFood() {
        Food jjajangmyeon = food(1L, "짜장면", "면류", "짜장면", 180.0, 23);
        Food jjajangRamen = food(2L, "짜장라면", "가공면류", "짜장라면", 420.0, 8);
        Food jjajangSauce = food(3L, "짜장소스", "소스류", "짜장소스", 120.0, 12);
        when(overrideProvider.findOverride("짜장면")).thenReturn(Optional.empty());
        when(aliasRepository.findBySearchName("짜장면")).thenReturn(List.of(alias(jjajangmyeon, "짜장면A"), alias(jjajangmyeon, "즉석 짜장면")));
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc("짜장면")).thenReturn(Optional.of(jjajangmyeon));
        when(aliasRepository.searchContains(eq("짜장면"), any(Pageable.class))).thenReturn(List.of(alias(jjajangmyeon, "배달 짜장면")));
        when(foodRepository.searchContains(eq("짜장면"), any(Pageable.class))).thenReturn(List.of(jjajangmyeon));
        when(aliasRepository.countByFood_Id(1L)).thenReturn(23L);

        NutritionDtos.FoodSearchResponse response = service.search("짜장면");

        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getRepresentativeName()).isEqualTo("짜장면");
        assertThat(response.getResults().get(0).getMatchedAliasCount()).isEqualTo(23L);

        when(overrideProvider.findOverride("짜장")).thenReturn(Optional.empty());
        when(aliasRepository.findBySearchName("짜장")).thenReturn(List.of());
        when(foodRepository.findFirstBySearchNameOrderBySourceCountDesc("짜장")).thenReturn(Optional.empty());
        when(aliasRepository.searchContains(eq("짜장"), any(Pageable.class))).thenReturn(List.of(
                alias(jjajangmyeon, "짜장면A"), alias(jjajangRamen, "짜장라면A"), alias(jjajangSauce, "짜장소스A")));
        when(foodRepository.searchContains(eq("짜장"), any(Pageable.class))).thenReturn(List.of(jjajangmyeon, jjajangRamen, jjajangSauce));
        when(aliasRepository.countByFood_Id(2L)).thenReturn(8L);
        when(aliasRepository.countByFood_Id(3L)).thenReturn(12L);

        NutritionDtos.FoodSearchResponse broadResponse = service.search("짜장");

        assertThat(broadResponse.getResults()).extracting(NutritionDtos.FoodSearchItemResponse::getDisplayCategory)
                .contains("면류", "가공면류", "소스류");
    }

    private Food food(Long id, String name, String category, String searchName, Double kcal, int sourceCount) {
        return Food.builder().id(id).name(name).category(category).searchName(searchName).servingUnit("100g")
                .calorie(kcal).carbohydrate(10.0).protein(5.0).fat(2.0).sourceCount(sourceCount).build();
    }

    private FoodAlias alias(Food food, String aliasName) {
        return FoodAlias.builder().food(food).aliasName(aliasName).originalName(aliasName).searchName(normalizer.toSearchName(aliasName)).category(food.getCategory()).build();
    }
}
