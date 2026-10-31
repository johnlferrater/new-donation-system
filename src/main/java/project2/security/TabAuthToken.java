package project2.security;

import java.time.Instant;

public class TabAuthToken {
    private final String token;
    private final String username;
    private final java.util.Set<String> authorities;
    private final Instant createdAt;
    private Instant expiresAt;
    private Instant lastAccessedAt;

    public TabAuthToken(String token, String username, java.util.Set<String> authorities, long ttlMinutes) {
        this.token = token;
        this.username = username;
        this.authorities = authorities == null ? java.util.Collections.emptySet() : new java.util.HashSet<>(authorities);
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastAccessedAt = now;
        this.expiresAt = now.plusSeconds(ttlMinutes * 60);
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public java.util.Set<String> getAuthorities() { return authorities; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getLastAccessedAt() { return lastAccessedAt; }
    public void touch(long ttlMinutes) {
        Instant now = Instant.now();
        this.lastAccessedAt = now;
        this.expiresAt = now.plusSeconds(ttlMinutes * 60);
    }
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}