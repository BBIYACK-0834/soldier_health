package com.teukgeupjeonsa.backend.food.importer;

import com.teukgeupjeonsa.backend.food.Food;
import com.teukgeupjeonsa.backend.food.FoodAlias;
import com.teukgeupjeonsa.backend.food.FoodAliasRepository;
import com.teukgeupjeonsa.backend.food.FoodRepository;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.nio.file.Path;

public class FoodImportApplication {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(FoodImportConfiguration.class)
                .web(WebApplicationType.NONE)
                .profiles("food-import")
                .properties("app.startup-seed.enabled=false")
                .run(args)) {
            Environment environment = context.getEnvironment();
            String file = environment.getProperty("app.food-import.file", "../food_data/foods_final_user_friendly_100g.xlsx");

            FoodImportResult result = context.getBean(FoodXlsxImporter.class).importXlsx(Path.of(file));

            System.out.println("식품 데이터 import 완료");
            System.out.println("foods 삽입 개수: " + result.foodCount());
            System.out.println("food_aliases 삽입 개수: " + result.aliasCount());
            System.out.println("잘못되었거나 중복되어 건너뛴 food 개수: " + result.skippedFoodCount());
            System.out.println("매핑 실패로 건너뛴 alias 개수: " + result.skippedAliasCount());
        }
    }

    @Profile("food-import")
    @SpringBootApplication(scanBasePackageClasses = FoodXlsxImporter.class)
    @EntityScan(basePackageClasses = {Food.class, FoodAlias.class})
    @EnableJpaRepositories(basePackageClasses = {FoodRepository.class, FoodAliasRepository.class})
    static class FoodImportConfiguration {
    }
}