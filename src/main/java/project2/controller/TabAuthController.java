package project2.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import project2.security.TabAuthToken;
import project2.security.TabAuthTokenService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class TabAuthController {

    private final TabAuthTokenService tokenService;
    private final String authMode;

    public TabAuthController(TabAuthTokenService tokenService, @Value("") String authMode) {
        this.tokenService = tokenService;
        this.authMode = authMode == null ? "SESSION" : authMode.toUpperCase();
    }

    @PostMapping("/token")
    public ResponseEntity<?> issueToken(Authentication authentication) {
        if (!"TOKEN".equals(authMode)) {
            return ResponseEntity.status(403).body(Map.of("error", "Token auth disabled"));
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        String username = authentication.getName();
        Set<String> authorities = new HashSet<>();
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            if (ga != null && ga.getAuthority() != null) {
                authorities.add(ga.getAuthority());
            }
        }
        TabAuthToken token = tokenService.generateToken(username, authorities);
        return ResponseEntity.ok(Map.of("token", token.getToken(), "username", username));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = request.getHeader("X-Tab-Auth-Token");
        if ("TOKEN".equals(authMode) && token != null && !token.isBlank()) {
            tokenService.revokeToken(token);
            return ResponseEntity.ok(Map.of("status", "logged_out"));
        }
        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(Authentication authentication) {
        if (!"TOKEN".equals(authMode)) {
            return ResponseEntity.status(403).body(Map.of("error", "Token auth disabled"));
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        tokenService.revokeAll();
        return ResponseEntity.ok(Map.of("status", "logged_out_all"));
    }
}