package project2.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Central Spring Security configuration.
 *
 * <ul>
 *   <li>Form-based login backed by {@link DatabaseUserDetailsService}.</li>
 *   <li>BCrypt password hashing.</li>
 *   <li>Role-based authorization: ADMIN vs USER.</li>
 *   <li>CSRF protection using a cookie token so the fetch-based pages can send it.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Public pages and static assets. */
    private static final String[] PUBLIC = {
            "/", "/login", "/css/**", "/js/**", "/images/**", "/favicon.ico"
    };

    /** Management pages that only administrators may open. */
    private static final String[] ADMIN_PAGES = {"/users", "/admins"};

    private final DatabaseUserDetailsService userDetailsService;

    public SecurityConfig(DatabaseUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");

        http
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC).permitAll()
                        // Management pages: administrators only.
                        .requestMatchers(ADMIN_PAGES).hasRole("ADMIN")

                        // --- Admin-only resources: full access requires ADMIN ---
                        .requestMatchers("/api/users/**", "/api/admins/**").hasRole("ADMIN")

                        // --- Admin-only write operations ---
                        .requestMatchers(HttpMethod.POST, "/api/goals/**", "/api/announcements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/goals/**", "/api/announcements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/goals/**", "/api/announcements/**").hasRole("ADMIN")

                        // Everything else requires a logged-in user.
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                );

        return http.build();
    }
}
