package project2.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicPagesRenderWithTheSharedLayout() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("navbar-app")));
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
        mockMvc.perform(get("/css/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("--theme-primary")));
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void authenticatedPagesRenderForRegularUsers() throws Exception {
        mockMvc.perform(get("/dashboard")).andExpect(status().isOk());
        mockMvc.perform(get("/goals")).andExpect(status().isOk());
        mockMvc.perform(get("/donation-records")).andExpect(status().isOk());
        mockMvc.perform(get("/donation-history")).andExpect(status().isOk());
        mockMvc.perform(get("/announcements")).andExpect(status().isOk());
        mockMvc.perform(get("/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminPagesRenderForAdministrators() throws Exception {
        mockMvc.perform(get("/users")).andExpect(status().isOk());
        mockMvc.perform(get("/admins")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/users"));
    }
}
