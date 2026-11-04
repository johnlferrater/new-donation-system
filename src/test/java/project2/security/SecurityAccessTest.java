package project2.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import project2.entity.DonationGoal;
import project2.entity.User;
import project2.repository.DonationGoalRepository;
import project2.repository.UserRepository;

import java.math.BigDecimal;
import java.sql.Date;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityAccessTest {

    private static final String RAW_PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonationGoalRepository goalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createLoginUser() {
        User user = new User();
        user.setUsername("session-user");
        user.setEmail("session-user@example.org");
        user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        user.setRole("USER");
        userRepository.save(user);
    }

    @Test
    void anonymousCanOpenHomeAndLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousDashboardRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void anonymousGoalsRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/goals"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void anonymousDonationRecordsRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/donation-records"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void anonymousDonationHistoryRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/donation-history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void anonymousAnnouncementsRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/announcements"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void anonymousProtectedApisReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/donation-history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/donation-history"));

        mockMvc.perform(get("/api/donation-records"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void regularUserCanReadGoalsHistoryAndAnnouncements() throws Exception {
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/donation-history"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void regularUserCannotReadDonationRecords() throws Exception {
        mockMvc.perform(get("/api/donation-records"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void regularUserCannotAccessAdminAreasOrWriteGoalsAndAnnouncements() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admins"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/goals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/goals/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/goals/1").with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/announcements/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/announcements/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanCreateGoal() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson()))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCanReadGoalsAndAnnouncementsApis() throws Exception {
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotCreateGoalsOrAnnouncements() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void anonymousCannotUpdateOrDeleteGoalsOrAnnouncements() throws Exception {
        mockMvc.perform(put("/api/goals/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(delete("/api/goals/1").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(put("/api/announcements/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(delete("/api/announcements/1").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void userCannotWriteAnnouncements() throws Exception {
        mockMvc.perform(post("/api/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementJson()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/announcements/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementJson()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/announcements/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanCreateAnnouncement() throws Exception {
        mockMvc.perform(post("/api/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementJson()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void administratorCanAccessProtectedPagesAndApis() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/donation-records"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/donation-history"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void stateChangingRequestsWithoutCsrfAreForbidden() throws Exception {
        DonationGoal goal = saveGoal();

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/goals/" + goal.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/goals/" + goal.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void stateChangingRequestsWithCsrfSucceed() throws Exception {
        DonationGoal goal = saveGoal();

        mockMvc.perform(post("/api/goals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson()))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/goals/" + goal.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/goals/" + goal.getId()).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedHomeRequestKeepsSessionAndAuthentication() throws Exception {
        MockHttpSession session = loginAs("session-user");

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk());

        assertFalse(session.isInvalid());
        SecurityContext context = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        assertNotNull(context);
        assertEquals("session-user", context.getAuthentication().getName());
        assertTrue(context.getAuthentication().isAuthenticated());
    }

    @Test
    void logoutWithCsrfInvalidatesSessionAndRedirectsToLogin() throws Exception {
        MockHttpSession session = loginAs("session-user");

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        assertTrue(session.isInvalid());
    }

    private MockHttpSession loginAs(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", RAW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private DonationGoal saveGoal() {
        DonationGoal goal = new DonationGoal();
        goal.setTitle("Goal");
        goal.setTargetAmount(new BigDecimal("100.00"));
        goal.setCurrentAmount(new BigDecimal("10.00"));
        goal.setStartDate(Date.valueOf("2026-01-01"));
        goal.setEndDate(Date.valueOf("2026-12-31"));
        goal.setStatus("Active");
        return goalRepository.save(goal);
    }

    private String goalJson() {
        return """
                {
                  "title": "Goal",
                  "description": "Goal description",
                  "targetAmount": 100.00,
                  "currentAmount": 10.00,
                  "startDate": "2026-01-01",
                  "endDate": "2026-12-31",
                  "status": "Active"
                }
                """;
    }

    private String announcementJson() {
        return """
                {
                  "title": "Announcement",
                  "message": "Announcement message",
                  "category": "General",
                  "status": "Published"
                }
                """;
    }
}
