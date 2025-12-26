package com.example.foreverhome.config;

import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.repository.AdminRepository;
import com.example.foreverhome.repository.UserRepository;
import com.example.foreverhome.domain.profile.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Seeds the super admin user on application startup.
 * The super admin email is configured via app.super-admin.email property.
 * If the user already exists, their role is updated to SUPER_ADMIN.
 * If the user doesn't exist, a new account is created with a random password.
 */
@Component
@Order(1) // Run before TestDataSeeder
public class SuperAdminSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.super-admin.email:}")
    private String superAdminEmail;

    public SuperAdminSeeder(UserRepository userRepository,
                            AdminRepository adminRepository,
                            PasswordEncoder passwordEncoder,
                            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (superAdminEmail == null || superAdminEmail.isBlank()) {
            logger.info("No super admin email configured (app.super-admin.email is empty). Skipping super admin seeding.");
            return;
        }

        // Check if the database schema is ready
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'app_users'",
                Integer.class);
            if (tableCount == null || tableCount == 0) {
                logger.warn("Database schema not ready (app_users table does not exist). Skipping super admin seeding.");
                return;
            }
        } catch (Exception e) {
            logger.warn("Failed to check database schema readiness: {}. Skipping super admin seeding.", e.getMessage());
            return;
        }

        String email = superAdminEmail.trim().toLowerCase();
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getRole() == UserRole.SUPER_ADMIN) {
                logger.info("Super admin already exists: {}", email);
            } else {
                // Update existing user to SUPER_ADMIN role via direct SQL
                // (Spring Data JDBC doesn't support updating enum columns easily)
                jdbcTemplate.update(
                    "UPDATE app_users SET role = ? WHERE id = ?",
                    UserRole.SUPER_ADMIN.name(), user.getId()
                );
                logger.info("Upgraded existing user to super admin: {}", email);

                // Ensure admin profile exists
                ensureAdminProfile(user);
            }
        } else {
            // Create new super admin user with a secure random password
            String randomPassword = generateSecurePassword();
            String encodedPassword = passwordEncoder.encode(randomPassword);

            User superAdmin = User.create(email, encodedPassword, UserRole.SUPER_ADMIN, "Super Admin");
            superAdmin.activate();
            superAdmin.markProfileComplete();
            superAdmin = userRepository.save(superAdmin);

            // Create admin profile
            Admin adminProfile = Admin.create(superAdmin.getId());
            adminRepository.save(adminProfile);

            logger.info("Created super admin account: {}. Password must be set via password reset.", email);
        }
    }

    private void ensureAdminProfile(User user) {
        if (adminRepository.findByUserId(user.getId()).isEmpty()) {
            Admin adminProfile = Admin.create(user.getId());
            adminRepository.save(adminProfile);
            logger.info("Created admin profile for super admin: {}", user.getEmail());
        }
    }

    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
