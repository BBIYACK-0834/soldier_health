package com.teukgeupjeonsa.foodimport;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.food.FoodAlias;
import com.teukgeupjeonsa.backend.food.FoodAliasRepository;
import com.teukgeupjeonsa.backend.food.FoodRepository;
import com.teukgeupjeonsa.backend.food.ManualFoodOverride;
import com.teukgeupjeonsa.backend.food.ManualFoodOverrideRepository;
import com.teukgeupjeonsa.backend.food.ServingDefault;
import com.teukgeupjeonsa.backend.food.ServingDefaultRepository;
import com.teukgeupjeonsa.backend.food.importer.FoodImportResult;
import com.teukgeupjeonsa.backend.food.importer.FoodXlsxImporter;
import com.teukgeupjeonsa.backend.nutrition.FoodNameNormalizer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.nio.file.Path;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = {
        Food.class,
        FoodAlias.class,
        ManualFoodOverride.class,
        ServingDefault.class
})
@EnableJpaRepositories(basePackageClasses = {
        FoodRepository.class,
        FoodAliasRepository.class,
        ManualFoodOverrideRepository.class,
        ServingDefaultRepository.class
})
@Import({
        FoodXlsxImporter.class,
        FoodNameNormalizer.class
})
public class FoodImportApplication {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(FoodImportApplication.class)
                .web(WebApplicationType.NONE)
                .properties("app.startup-seed.enabled=false")
                .run(args)) {

            Environment environment = context.getEnvironment();
            String file = environment.getProperty(
                    "app.food-import.file",
                    "../food_data/foods_refined_for_military_meal_matching.xlsx"
            );

            FoodImportResult result = context.getBean(FoodXlsxImporter.class).importXlsx(Path.of(file));

            System.out.println("식품 데이터 import 완료");
            System.out.println("foods 삽입 개수: " + result.foodCount());
            System.out.println("food_aliases 삽입 개수: " + result.aliasCount());
            System.out.println("잘못되었거나 중복되어 건너뛴 food 개수: " + result.skippedFoodCount());
            System.out.println("매핑 실패로 건너뛴 alias 개수: " + result.skippedAliasCount());
            System.out.println("manual_overrides 삽입 개수: " + result.overrideCount());
            System.out.println("serving_defaults 삽입 개수: " + result.servingDefaultCount());
        }
    }
}
