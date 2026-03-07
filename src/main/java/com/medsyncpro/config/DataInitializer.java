package com.medsyncpro.config;

import com.medsyncpro.entity.DocumentType;
import com.medsyncpro.entity.Role;
import com.medsyncpro.repository.DocumentTypeRepository;
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
    private final DocumentTypeRepository documentTypeRepository;
    
    @Override
    public void run(String... args) {
        log.info("Running data initialization...");
        adminInitializationService.initializeDefaultAdmin();
        initializeDocumentTypes();
    }
    
    private void initializeDocumentTypes() {
        if (documentTypeRepository.count() > 0) {
            log.info("Document types already initialized");
            return;
        }
        
        log.info("Initializing default document types for DOCTOR role...");
        
        documentTypeRepository.save(DocumentType.builder()
            .role(Role.DOCTOR)
            .name("Medical License")
            .code("MEDICAL_LICENSE")
            .description("Valid medical license issued by medical council")
            .required(true)
            .active(true)
            .displayOrder(1)
            .build());
            
        documentTypeRepository.save(DocumentType.builder()
            .role(Role.DOCTOR)
            .name("Medical Degree Certificate")
            .code("DEGREE_CERTIFICATE")
            .description("MBBS or equivalent medical degree certificate")
            .required(true)
            .active(true)
            .displayOrder(2)
            .build());
            
        documentTypeRepository.save(DocumentType.builder()
            .role(Role.DOCTOR)
            .name("Identity Proof")
            .code("IDENTITY_PROOF")
            .description("Government issued ID (Aadhaar, Passport, etc.)")
            .required(true)
            .active(true)
            .displayOrder(3)
            .build());
            
        documentTypeRepository.save(DocumentType.builder()
            .role(Role.DOCTOR)
            .name("Specialization Certificate")
            .code("SPECIALIZATION_CERT")
            .description("Post-graduation or specialization certificate (if applicable)")
            .required(false)
            .active(true)
            .displayOrder(4)
            .build());
            
        log.info("Document types initialized successfully");
    }
}
