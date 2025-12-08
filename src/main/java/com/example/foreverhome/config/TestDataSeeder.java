package com.example.foreverhome.config;

import com.example.foreverhome.domain.user.User;
import com.example.foreverhome.domain.user.UserRole;
import com.example.foreverhome.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds test accounts when test mode is enabled.
 * Each role gets a dedicated test account with a known password.
 */
@Component
@ConditionalOnProperty(name = "app.test-mode.enabled", havingValue = "true")
public class TestDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestDataSeeder.class);
    private static final String TEST_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TestDataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        logger.info("Test mode enabled - seeding test accounts");

        List<TestAccount> testAccounts = List.of(
            new TestAccount("admin@test.com", "Test Admin", UserRole.ADMIN),
            new TestAccount("foster@test.com", "Test Foster", UserRole.FOSTER),
            new TestAccount("adopter@test.com", "Test Adopter", UserRole.ADOPTER),
            new TestAccount("vet@test.com", "Test Vet", UserRole.VET),
            new TestAccount("rescue@test.com", "Test Rescue Org", UserRole.RESCUE_ORG)
        );

        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);

        for (TestAccount account : testAccounts) {
            if (!userRepository.existsByEmail(account.email())) {
                User user = User.create(account.email(), encodedPassword, account.role(), account.name());
                user.activate();
                user.markProfileComplete();
                userRepository.save(user);
                logger.info("Created test account: {} ({})", account.email(), account.role());
            } else {
                logger.debug("Test account already exists: {}", account.email());
            }
        }

        logger.info("Test account seeding complete. All accounts use password: {}", TEST_PASSWORD);
    }

    private record TestAccount(String email, String name, UserRole role) {}
}
