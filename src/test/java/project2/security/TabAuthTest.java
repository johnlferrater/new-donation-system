package project2.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import project2.entity.User;
import project2.repository.UserRepository;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Server-side per-tab token support only. The browser client is not implemented:
 * nothing in app.js/templates requests a token, stores it in sessionStorage, or sends
 * X-Tab-Auth-Token, so per-tab login is not available to users yet. These tests pin
 * down the server-side behavior in app.auth.mode=TOKEN.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "app.auth.mode=TOKEN")
class TabAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TabAuthTokenService tokenService;

    @Test
    void eachTokenResolvesToItsOwnUser() throws Exception {
        saveUser("alice", "USER");
        saveUser("bob", "ADMIN");
        String aliceToken = tokenService.generateToken("alice", Set.of("ROLE_USER")).getToken();
        String bobToken = tokenService.generateToken("bob", Set.of("ROLE_ADMIN")).getToken();

        mockMvc.perform(get("/api/users/me").header("X-Tab-Auth-Token", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));

        mockMvc.perform(get("/api/users/me").header("X-Tab-Auth-Token", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        saveUser("alice", "USER");

        mockMvc.perform(get("/api/users/me").header("X-Tab-Auth-Token", "not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        saveUser("alice", "USER");
        TabAuthToken token = tokenService.generateToken("alice", Set.of("ROLE_USER"));
        Field expiresAt = TabAuthToken.class.getDeclaredField("expiresAt");
        expiresAt.setAccessible(true);
        expiresAt.set(token, Instant.now().minusSeconds(60));

        mockMvc.perform(get("/api/users/me").header("X-Tab-Auth-Token", token.getToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void logoutRevokesOnlyTheGivenToken() throws Exception {
        saveUser("alice", "USER");
        String first = tokenService.generateToken("alice", Set.of("ROLE_USER")).getToken();
        String second = tokenService.generateToken("alice", Set.of("ROLE_USER")).getToken();

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .header("X-Tab-Auth-Token", first))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me").header("X-Tab-Auth-Token", first))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me").header("X-Tab-Auth-Token", second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void invalidTokenWithValidSessionIsStillRejected() throws Exception {
        saveUser("alice", "USER");
        MockHttpSession session = loginAs("alice");

        mockMvc.perform(get("/api/users/me")
                        .session(session)
                        .header("X-Tab-Auth-Token", "not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void validTokenOverridesDifferentUsersSession() throws Exception {
        saveUser("alice", "USER");
        saveUser("bob", "ADMIN");
        MockHttpSession session = loginAs("alice");
        String bobToken = tokenService.generateToken("bob", Set.of("ROLE_ADMIN")).getToken();

        mockMvc.perform(get("/api/users/me")
                        .session(session)
                        .header("X-Tab-Auth-Token", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    void logoutWithBadTokenStillReturns200() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .header("X-Tab-Auth-Token", "not-a-real-token"))
                .andExpect(status().isOk());
    }

    @Test
    void stateChangingRequestWithTokenButNoCsrfIsForbidden() throws Exception {
        saveUser("bob", "ADMIN");
        String token = tokenService.generateToken("bob", Set.of("ROLE_ADMIN")).getToken();

        mockMvc.perform(post("/api/goals")
                        .header("X-Tab-Auth-Token", token)
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Goal",
                                  "targetAmount": 100.00,
                                  "currentAmount": 10.00,
                                  "startDate": "2026-01-01",
                                  "endDate": "2026-12-31",
                                  "status": "Active"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession loginAs(String username) throws Exception {
        var result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void saveUser(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.org");
        user.setPassword(passwordEncoder.encode("secret123"));
        user.setRole(role);
        userRepository.save(user);
    }
}
