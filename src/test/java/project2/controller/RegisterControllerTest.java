package project2.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import project2.entity.User;
import project2.repository.UserRepository;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "app.admin-signup-code=test-admin-code")
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registeringWithoutRoleCreatesUser() throws Exception {
        register("no-role-user", null, null)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        assertEquals("USER", storedRole("no-role-user"));
    }

    @Test
    void registeringWithAdminRoleAndCorrectCodeCreatesAdmin() throws Exception {
        register("new-admin", "ADMIN", "test-admin-code")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        assertEquals("ADMIN", storedRole("new-admin"));
    }

    @Test
    void registeringWithAdminRoleAndWrongCodeIsRejected() throws Exception {
        register("wrong-code-admin", "ADMIN", "not-the-code")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid admin code")));

        assertFalse(userRepository.existsById("wrong-code-admin"));
    }

    @Test
    void registeringWithAdminRoleAndNoCodeIsRejected() throws Exception {
        register("missing-code-admin", "ADMIN", null)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid admin code")));

        assertFalse(userRepository.existsById("missing-code-admin"));
    }

    @Test
    void craftedAdminRequestWithoutCodeCannotCreateAdmin() throws Exception {
        register("crafted-admin", "admin", " ")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid admin code")));

        assertFalse(userRepository.existsById("crafted-admin"));
    }

    @Test
    void unrecognizedRoleDefaultsToUser() throws Exception {
        register("unknown-role-user", "SUPERUSER", null)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        assertEquals("USER", storedRole("unknown-role-user"));
    }

    private ResultActions register(String username, String role, String adminCode) throws Exception {
        MockHttpServletRequestBuilder request = post("/register")
                .with(csrf())
                .param("username", username)
                .param("email", username + "@example.org")
                .param("password", "secret123")
                .param("confirmPassword", "secret123");
        if (role != null) {
            request.param("role", role);
        }
        if (adminCode != null) {
            request.param("adminCode", adminCode);
        }
        return mockMvc.perform(request);
    }

    private String storedRole(String username) {
        User user = userRepository.findById(username).orElseThrow();
        return user.getRole();
    }
}
