package project2.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TabAuthFilter extends OncePerRequestFilter {

    private final TabAuthTokenService tokenService;
    private final String authMode;

    public TabAuthFilter(TabAuthTokenService tokenService, @Value("") String authMode) {
        this.tokenService = tokenService;
        this.authMode = authMode == null ? "SESSION" : authMode.toUpperCase();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"TOKEN".equals(authMode)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("X-Tab-Auth-Token");
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        TabAuthToken tabToken = tokenService.validateToken(token);
        if (tabToken != null) {
            var auth = tokenService.toAuthentication(tabToken);
            if (auth != null) {
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}