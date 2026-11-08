package com.replica.bellavistaerp.auth;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates the first Head Office Admin when the users table is empty, so
 * someone can sign in and approve everybody else.
 * Change the password after the first login (or set BV_ADMIN_PASSWORD first).
 */
@Configuration
@RequiredArgsConstructor
public class AdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AppUserRepository users;
    private final PasswordHasher hasher;

    @Bean
    CommandLineRunner initialiseAdmin(
            @Value("${app.auth.admin-email:ho@bellavista.ac.zw}") String email,
            @Value("${app.auth.admin-password:Bella@2026}") String password) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            AppUser admin = new AppUser();
            admin.setFullName("Head Office Admin");
            admin.setStaffNumber("BV-0003");
            admin.setEmail(email.toLowerCase());
            admin.setDepartment("Head Office");
            admin.setRole(UserRole.HO);
            admin.setPasswordHash(hasher.hash(password));
            admin.setActive(true);
            users.save(admin);
            log.warn("Created first Head Office Admin account: {} (change its password).", email);
        };
    }
}
