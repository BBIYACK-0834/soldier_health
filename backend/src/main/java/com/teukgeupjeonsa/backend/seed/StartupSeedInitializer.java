package com.teukgeupjeonsa.backend.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.startup-seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class StartupSeedInitializer implements CommandLineRunner {

    private final SeedService seedService;

    @Value("${app.startup-seed.fail-on-error:false}")
    private boolean failOnError;

    @Override
    public void run(String... args) {
        try {
            String result = seedService.seedSampleData();
            log.info(result);
        } catch (RuntimeException e) {
            if (failOnError) {
                throw e;
            }
            log.error("Startup seed failed, but application startup will continue. "
                    + "Set app.startup-seed.fail-on-error=true to fail fast.", e);
        }
    }
}
