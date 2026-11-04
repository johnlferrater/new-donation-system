package project2.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import project2.repository.UserRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegisterAdminSignupDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void adminRoleWhenConfiguredCodeIsBlankIsRejected() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "blank-config-admin")
                        .param("email", "blank-config-admin@example.org")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("role", "ADMIN")
                        .param("adminCode", "anything"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid admin code")));

        assertFalse(userRepository.existsById("blank-config-admin"));
    }

    @Test
    void registerPageHidesAdminOptionWhenConfiguredCodeIsBlank() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("value=\"ADMIN\""))));
    }
}
