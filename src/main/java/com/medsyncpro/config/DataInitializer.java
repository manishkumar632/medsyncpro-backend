package com.medsyncpro.config;

import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "admin@medsyncpro.com";

        if (userRepository.findByEmail(adminEmail) == null) {
            User admin = new User();
            admin.setName("Super Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole(Role.ADMIN);
            admin.setProfessionalVerificationStatus(com.medsyncpro.entity.VerificationStatus.VERIFIED);
            admin.setEmailVerified(true);
            admin.setDeleted(false);

            userRepository.save(admin);
            log.info("✅ Default admin created — email: {}", adminEmail);
        } else {
            log.info("Admin user already exists, skipping creation");
        }
    }
}
