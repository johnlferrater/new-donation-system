package project2.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import project2.entity.User;
import project2.repository.UserRepository;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @WithMockUser(username = "acting-admin", roles = "ADMIN")
    void lastAdministratorCannotBeDemoted() throws Exception {
        saveUser("only-admin", "ADMIN", "admin-password-123");

        mockMvc.perform(put("/api/users/only-admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("last administrator")));

        assertTrue(userRepository.findById("only-admin").orElseThrow().getRole().equals("ADMIN"));
    }

    @Test
    @WithMockUser(username = "acting-admin", roles = "ADMIN")
    void lastAdministratorCannotBeDeleted() throws Exception {
        saveUser("only-admin", "ADMIN", "admin-password-123");

        mockMvc.perform(delete("/api/users/only-admin").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("last administrator")));

        assertTrue(userRepository.existsById("only-admin"));
    }

    @Test
    @WithMockUser(username = "self-admin", roles = "ADMIN")
    void administratorCannotDemoteThemselves() throws Exception {
        saveUser("self-admin", "ADMIN", "admin-password-123");
        saveUser("other-admin", "ADMIN", "admin-password-456");

        mockMvc.perform(put("/api/users/self-admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("own administrator")));

        assertTrue(userRepository.findById("self-admin").orElseThrow().getRole().equals("ADMIN"));
    }

    @Test
    @WithMockUser(username = "self-admin", roles = "ADMIN")
    void administratorCannotDeleteThemselves() throws Exception {
        saveUser("self-admin", "ADMIN", "admin-password-123");

        mockMvc.perform(delete("/api/users/self-admin").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("own account")));

        assertTrue(userRepository.existsById("self-admin"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createdUserPasswordIsHashedAndNotExposedInJson() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "created-user",
                                  "email": "created-user@example.org",
                                  "password": "secret123",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());

        User stored = userRepository.findById("created-user").orElseThrow();
        assertNotEquals("secret123", stored.getPassword());
        assertFalse(stored.getPassword().contains("secret123"));
        assertTrue(passwordEncoder.matches("secret123", stored.getPassword()));

        mockMvc.perform(get("/api/users/created-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    private void saveUser(String username, String role, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.org");
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);
    }
}
