package project2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import project2.entity.User;
import project2.repository.UserRepository;

import java.util.logging.Logger;

/**
 * Creates the first administrator only when credentials are supplied through
 * environment-backed application properties. There are no built-in accounts.
 */
@Configuration
public class DataSeeder {

    private static final Logger LOGGER = Logger.getLogger(DataSeeder.class.getName());

    @Bean
    public CommandLineRunner seedUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.username:}") String adminUsername,
            @Value("${app.bootstrap.admin.email:}") String adminEmail,
            @Value("${app.bootstrap.admin.password:}") String adminPassword) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            boolean allMissing = adminUsername.isBlank() && adminEmail.isBlank() && adminPassword.isBlank();
            if (allMissing) {
                LOGGER.warning("No users exist and no bootstrap administrator is configured. "
                        + "Set APP_BOOTSTRAP_ADMIN_USERNAME, APP_BOOTSTRAP_ADMIN_EMAIL, "
                        + "and APP_BOOTSTRAP_ADMIN_PASSWORD before creating the first administrator.");
                return;
            }
            if (adminUsername.isBlank() || adminEmail.isBlank() || adminPassword.isBlank()) {
                throw new IllegalStateException("All APP_BOOTSTRAP_ADMIN_* settings must be configured together.");
            }
            if (adminPassword.length() < 12) {
                throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters.");
            }

            User admin = new User();
            admin.setUsername(adminUsername.trim());
            admin.setEmail(adminEmail.trim());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            userRepository.save(admin);

            LOGGER.info("Created the initial administrator from environment configuration.");
        };
    }
}
