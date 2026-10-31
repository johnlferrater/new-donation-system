package project2.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@EnableScheduling
public class TabAuthTokenService {

    private final ConcurrentHashMap<String, TabAuthToken> tokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final long ttlMinutes;

    public TabAuthTokenService(@Value("${app.tabauth.token-ttl-minutes:30}") long ttlMinutes) {
        if (ttlMinutes <= 0) {
            throw new IllegalArgumentException("app.tabauth.token-ttl-minutes must be positive");
        }
        this.ttlMinutes = ttlMinutes;
    }

    public TabAuthToken generateToken(String username, Set<String> authorities) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        TabAuthToken tabToken = new TabAuthToken(token, username, authorities, ttlMinutes);
        tokens.put(token, tabToken);
        return tabToken;
    }

    public TabAuthToken validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        TabAuthToken tabToken = tokens.get(token);
        if (tabToken == null) {
            return null;
        }
        if (tabToken.isExpired()) {
            tokens.remove(token);
            return null;
        }
        tabToken.touch(ttlMinutes);
        return tabToken;
    }

    public void revokeToken(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }

    public void revokeAll() {
        tokens.clear();
    }

    public UsernamePasswordAuthenticationToken toAuthentication(TabAuthToken tabToken) {
        if (tabToken == null) {
            return null;
        }
        Set<SimpleGrantedAuthority> authorities = tabToken.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
        return new UsernamePasswordAuthenticationToken(tabToken.getUsername(), null, authorities);
    }

    @Scheduled(fixedDelayString = "${app.tabauth.cleanup-interval-ms:60000}")
    public void cleanup() {
        tokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public int activeCount() {
        return tokens.size();
    }
}