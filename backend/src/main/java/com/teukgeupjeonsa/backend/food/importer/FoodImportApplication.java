package com.teukgeupjeonsa.backend.food.importer;

import com.teukgeupjeonsa.backend.TeukgeupjeonsaBackendApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

public class FoodImportApplication {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TeukgeupjeonsaBackendApplication.class)
                .web(WebApplicationType.NONE)
                .properties("app.startup-seed.enabled=false")
                .run(args)) {
            Environment environment = context.getEnvironment();
            String file = environment.getProperty("app.food-import.file", "../food_data/food_data_DB.xlsx");
            FoodImportResult result = context.getBean(FoodXlsxImporter.class).importXlsx(Path.of(file));
            System.out.println("식품 데이터 import 완료");
            System.out.println("foods 삽입 개수: " + result.foodCount());
            System.out.println("food_aliases 삽입 개수: " + result.aliasCount());
            System.out.println("매핑 실패로 건너뛴 alias 개수: " + result.skippedAliasCount());
        }
    }
}
