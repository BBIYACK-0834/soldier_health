package com.teukgeupjeonsa.menuimport;

import com.teukgeupjeonsa.backend.nutrition.menu.*;
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
@EntityScan(basePackageClasses = {MilitaryMenuProfile.class, MilitaryMenuUnitProfile.class, MilitaryMenuDailyProfile.class})
@EnableJpaRepositories(basePackageClasses = {
        MilitaryMenuProfileRepository.class, MilitaryMenuUnitProfileRepository.class, MilitaryMenuDailyProfileRepository.class})
@Import({MilitaryMenuXlsxImporter.class, MilitaryMenuDailyCsvImporter.class, MilitaryNutritionDataImporter.class})
public class MilitaryMenuImportApplication {
    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(MilitaryMenuImportApplication.class)
                .web(WebApplicationType.NONE).properties("app.startup-seed.enabled=false").run(args)) {
            Environment environment = context.getEnvironment();
            String file = environment.getProperty("app.military-menu-import.file", "src/main/resources/food_data/military_menu_data.xlsx");
            String dailyFile = environment.getProperty("app.military-menu-import.daily-file",
                    "src/main/resources/food_data/military_menu_daily_profiles.csv.gz");
            MilitaryMenuImportResult result = context.getBean(MilitaryNutritionDataImporter.class)
                    .importAll(Path.of(file), Path.of(dailyFile));
            System.out.println("군 급식 메뉴 데이터 import 완료");
            System.out.println("menu profiles 삽입 개수: " + result.menuCount());
            System.out.println("unit profiles 삽입 개수: " + result.unitProfileCount());
            System.out.println("daily profiles 삽입 개수: " + result.dailyProfileCount());
            System.out.println("건너뛴 menu 개수: " + result.skippedMenuCount());
            System.out.println("건너뛴 unit profile 개수: " + result.skippedUnitProfileCount());
        }
    }
}
