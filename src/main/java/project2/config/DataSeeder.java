package project2.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import project2.entity.User;
import project2.repository.UserRepository;

/**
 * Seeds default accounts on startup when the user table is empty.
 * Credentials are development defaults and must be changed in production.
 */
@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@donation.local");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);

            User user = new User();
            user.setUsername("user");
            user.setEmail("user@donation.local");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRole("USER");
            userRepository.save(user);

            System.out.println("[DataSeeder] Created default users: admin/admin123 (ADMIN), user/user123 (USER)");
        };
    }
}
