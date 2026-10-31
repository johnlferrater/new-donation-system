package project2.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import project2.repository.UserRepository;
import java.io.IOException;

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
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    /** Public pages and static assets. */
    private static final String[] PUBLIC = {
        "/", "/login", "/register", "/css/**", "/js/**", "/images/**", "/favicon.ico",
        "/goals", "/announcements", "/api/goals", "/api/announcements"
};

    /** Management pages that only administrators may open. */
    private static final String[] ADMIN_PAGES = {"/users", "/admins"};

    private final DatabaseUserDetailsService userDetailsService;
    private final boolean secureCookies;
    private final UserRepository userRepository;

    public SecurityConfig(
            DatabaseUserDetailsService userDetailsService,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookies,
            UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.secureCookies = secureCookies;
        this.userRepository = userRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TabAuthFilter tabAuthFilter) throws Exception {
        // Client code reads the token from a server-rendered meta tag, not this cookie.
        CookieCsrfTokenRepository csrfTokenRepository = new CookieCsrfTokenRepository();
        csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie
                .secure(secureCookies)
                .sameSite("Lax"));

        http
                .userDetailsService(userDetailsService)
                .addFilterBefore(tabAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Public donation submissions
                        .requestMatchers(HttpMethod.POST, "/api/donation-records", "/api/donation-history").permitAll()
                        // Management pages: administrators only.
                        .requestMatchers(ADMIN_PAGES).hasRole("ADMIN")

                        // --- User profile endpoints ---
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/users/me/change-password").authenticated()
                        // --- Admin-only resources: full access requires ADMIN ---
                        .requestMatchers("/api/users", "/api/admins/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasRole("ADMIN")

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
                        .successHandler(portalAuthenticationSuccessHandler())
                        .failureHandler(portalAuthenticationFailureHandler())
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

    @Bean
    public AuthenticationSuccessHandler portalAuthenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler("/dashboard") {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                String portal = request.getParameter("loginPortal");
                String username = authentication.getName();
                var userOpt = userRepository.findById(username);
                if (userOpt.isPresent()) {
                    String role = userOpt.get().getRole();
                    boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
                    if ("admin".equalsIgnoreCase(portal) && !isAdmin) {
                        // Reject - treat as failure
                        new org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler("/login?error=portal")
                                .onAuthenticationFailure(request, response, new org.springframework.security.authentication.BadCredentialsException("Portal mismatch"));
                        return;
                    }
                    if ("user".equalsIgnoreCase(portal) && isAdmin) {
                        new org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler("/login?error=portal")
                                .onAuthenticationFailure(request, response, new org.springframework.security.authentication.BadCredentialsException("Portal mismatch"));
                        return;
                    }
                }
                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler portalAuthenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                org.springframework.security.core.AuthenticationException exception) throws IOException, ServletException {
                String portal = request.getParameter("loginPortal");
                String username = request.getParameter("username");
                if ("admin".equalsIgnoreCase(portal) && username != null) {
                    var userOpt = userRepository.findById(username);
                    if (userOpt.isPresent() && !"ADMIN".equalsIgnoreCase(userOpt.get().getRole())) {
                        super.setDefaultFailureUrl("/login?error=portal");
                        super.onAuthenticationFailure(request, response, exception);
                        return;
                    }
                }
                if ("user".equalsIgnoreCase(portal) && username != null) {
                    var userOpt = userRepository.findById(username);
                    if (userOpt.isPresent() && "ADMIN".equalsIgnoreCase(userOpt.get().getRole())) {
                        super.setDefaultFailureUrl("/login?error=portal");
                        super.onAuthenticationFailure(request, response, exception);
                        return;
                    }
                }
                super.setDefaultFailureUrl("/login?error");
                super.onAuthenticationFailure(request, response, exception);
            }
        };
    }
    
}
