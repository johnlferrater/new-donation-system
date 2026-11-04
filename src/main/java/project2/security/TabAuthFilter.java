package project2.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import project2.exception.ApiError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class TabAuthFilter extends OncePerRequestFilter {

    private final TabAuthTokenService tokenService;
    private final ObjectMapper objectMapper;
    private final String authMode;

    public TabAuthFilter(TabAuthTokenService tokenService, ObjectMapper objectMapper,
                         @Value("${app.auth.mode:SESSION}") String authMode) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
        this.authMode = authMode == null ? "SESSION" : authMode.toUpperCase();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // SESSION mode: the header is ignored completely (session cookie decides identity).
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
        if (tabToken == null) {
            // Logout must stay forgiving so an expired/revoked tab can still log out cleanly.
            if (isLogoutRequest(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            // Fail closed: a bad tab token is rejected instead of falling back to session identity.
            writeUnauthorized(request, response);
            return;
        }
        var auth = tokenService.toAuthentication(tabToken);
        if (auth != null) {
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return HttpMethod.POST.matches(request.getMethod()) && "/api/auth/logout".equals(path);
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(),
                new ApiError(401, "Unauthorized", "Invalid or expired tab token", request.getRequestURI()));
    }
}