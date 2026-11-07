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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import project2.repository.UserRepository;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

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
        "/goals", "/announcements",
        // Public diagnostic endpoint protected by its own rate limit filter.
        "/api/rate-limit-test"
};

    /** Public API read endpoints (GET only), including goal/announcement detail lookups. */
    private static final String[] PUBLIC_API_READ = {"/api/goals", "/api/goals/**", "/api/announcements"};

    /** Management pages that only administrators may open. */
    private static final String[] ADMIN_PAGES = {"/users", "/admins"};

    private final DatabaseUserDetailsService userDetailsService;
    private final boolean secureCookies;
    private final UserRepository userRepository;
    private final List<String> corsAllowedOrigins;
    private final String csrfCookieSameSite;

    public SecurityConfig(
            DatabaseUserDetailsService userDetailsService,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secureCookies,
            UserRepository userRepository,
            @Value("${cors.allowed-origins:http://localhost:8080,http://localhost:3000}")
                    String corsAllowedOrigins,
            @Value("${app.csrf.cookie.same-site:Lax}") String csrfCookieSameSite) {
        this.userDetailsService = userDetailsService;
        this.secureCookies = secureCookies;
        this.userRepository = userRepository;
        this.corsAllowedOrigins = corsAllowedOrigins == null || corsAllowedOrigins.isBlank()
                ? List.of()
                : Arrays.stream(corsAllowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .collect(Collectors.toList());
        this.csrfCookieSameSite = csrfCookieSameSite == null || csrfCookieSameSite.isBlank()
                ? "Lax"
                : csrfCookieSameSite.trim();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        if (corsAllowedOrigins.isEmpty()) {
            return request -> null;
        }
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Accept", "Authorization", "Content-Type", "Origin", "X-Requested-With", "X-XSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TabAuthFilter tabAuthFilter,
                                           ApiSecurityErrorHandler apiSecurityErrorHandler) throws Exception {
        // Client code reads the token from a server-rendered meta tag, not this cookie.
        CookieCsrfTokenRepository csrfTokenRepository = new CookieCsrfTokenRepository();
        csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie
                .secure(secureCookies)
                .sameSite(csrfCookieSameSite));

        RequestMatcher apiRequests = request -> {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            return path.startsWith("/api/");
        };

        // Browser requests get a relative redirect so redirects keep working behind proxies.
        LoginUrlAuthenticationEntryPoint loginEntryPoint = new LoginUrlAuthenticationEntryPoint("/login") {
            @Override
            protected String buildRedirectUrlToLoginPage(HttpServletRequest request, HttpServletResponse response,
                                                         AuthenticationException authException) {
                String loginFormUrl = getLoginFormUrl();
                if (UrlUtils.isAbsoluteUrl(loginFormUrl)) {
                    return loginFormUrl;
                }
                return request.getContextPath() + loginFormUrl;
            }
        };

        // API requests must return JSON 401; browser pages still redirect to the login page.
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        entryPoints.put(apiRequests, apiSecurityErrorHandler);
        entryPoints.put(new NegatedRequestMatcher(apiRequests), loginEntryPoint);
        DelegatingAuthenticationEntryPoint authenticationEntryPoint =
                new DelegatingAuthenticationEntryPoint(entryPoints);
        authenticationEntryPoint.setDefaultEntryPoint(loginEntryPoint);

        http
                .userDetailsService(userDetailsService)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .addFilterBefore(tabAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_API_READ).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Donation submissions require a logged-in user.
                        .requestMatchers(HttpMethod.POST, "/api/donation-records", "/api/donation-history").authenticated()
                        // Management pages: administrators only.
                        .requestMatchers(ADMIN_PAGES).hasRole("ADMIN")

                        // --- Donation resources ---
                        .requestMatchers(HttpMethod.GET, "/api/donation-records/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/donation-records/**", "/api/donation-history/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/donation-records/**", "/api/donation-history/**").hasRole("ADMIN")

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
                        .requestMatchers(HttpMethod.POST, "/api/goals", "/api/goals/**", "/api/announcements", "/api/announcements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/goals", "/api/goals/**", "/api/announcements", "/api/announcements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/goals", "/api/goals/**", "/api/announcements", "/api/announcements/**").hasRole("ADMIN")

                        // Everything else requires a logged-in user.
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        // API requests: structured JSON errors.
                        .defaultAccessDeniedHandlerFor(apiSecurityErrorHandler, apiRequests)
                        // API requests get JSON 401; browser pages redirect to the login page.
                        .authenticationEntryPoint(authenticationEntryPoint)
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
