package com.medsyncpro.config;

import com.medsyncpro.service.AdminInitializationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AdminInitializationService adminInitializationService;
    @Override
    public void run(String... args) {
        log.info("Running data initialization...");
        adminInitializationService.initializeDefaultAdmin();
    }
}
