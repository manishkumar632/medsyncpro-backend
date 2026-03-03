package com.medsyncpro.service;

import com.medsyncpro.entity.Admin;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.repository.AdminRepository;
import com.medsyncpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for initializing the default Admin user at application
 * startup.
 * <p>
 * This service ensures that exactly one default admin exists in the system.
 * Admin credentials are configurable via environment variables for production
 * safety.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminInitializationService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@medsyncpro.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    /**
     * Initializes the default admin user if no admin exists in the system.
     * Creates both a User record (with ADMIN role) and an Admin profile record.
     * The operation is transactional — both records are created or neither is.
     */
    @Transactional
    public void initializeDefaultAdmin() {
        if (userRepository.existsByRoleAndDeletedFalse(Role.ADMIN)) {
            log.info("Admin user already exists, skipping creation");
            return;
        }

        log.info("No admin user found. Creating default admin with email: {}", adminEmail);

        // 1. Create the User entity (auth record)
        User adminUser = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .emailVerified(true)
                .phoneVerified(false)
                .termsAccepted(true)
                .build();

        adminUser = userRepository.save(adminUser);
        log.debug("Created admin User entity with id: {}", adminUser.getId());

        // 2. Create the Admin entity (profile record)
        Admin adminProfile = Admin.builder()
                .user(adminUser)
                .name("System Admin")
                .department("Administration")
                .level("SUPER_ADMIN")
                .build();

        adminRepository.save(adminProfile);

        log.info("✅ Default admin created — email: {}, level: SUPER_ADMIN", adminEmail);
    }
}
