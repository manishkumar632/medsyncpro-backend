package com.medsyncpro.config;

import com.medsyncpro.service.AdminInitializationService;
import com.medsyncpro.service.DocumentTypeDataInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AdminInitializationService adminInitializationService;
    private final DocumentTypeDataInitializer documentTypeDataInitializer;

    @Override
    public void run(String... args) {
        log.info("Running data initialization...");
        adminInitializationService.initializeDefaultAdmin();
        documentTypeDataInitializer.seedDefaults();
        log.info("Data initialization complete.");
    }
}
