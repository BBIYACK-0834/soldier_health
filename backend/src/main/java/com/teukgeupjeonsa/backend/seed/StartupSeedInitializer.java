package com.teukgeupjeonsa.backend.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.startup-seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class StartupSeedInitializer implements CommandLineRunner {

    private final SeedService seedService;

    @Override
    public void run(String... args) {
        String result = seedService.seedSampleData();
        log.info(result);
    }
}
