package project2.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
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
@WithMockUser(username = "admin", roles = "ADMIN")
class ValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void goalWithoutTitleReturnsBadRequestWithFieldDetails() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetAmount": 100.00,
                                  "currentAmount": 0.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/goals"))
                .andExpect(jsonPath("$.details", hasItem(containsString("title"))));
    }

    @Test
    void goalWithZeroTargetAmountReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Zero target",
                                  "targetAmount": 0.00,
                                  "currentAmount": 0.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("targetAmount"))));
    }

    @Test
    void goalWithNegativeTargetAmountReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Negative target",
                                  "targetAmount": -1.00,
                                  "currentAmount": 0.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("targetAmount"))));
    }

    @Test
    void donationRecordWithoutDonorNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/donation-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("""
                                "dateDonated": "2026-01-01",
                                "donorAddress": "Address",
                                "donationType": "Cash donation",
                                "amount": 10.00,
                                "paymentMethod": "Cash"
                                """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("donorName"))));
    }

    @Test
    void donationRecordWithoutAmountReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/donation-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("""
                                "donorName": "Donor",
                                "dateDonated": "2026-01-01",
                                "donorAddress": "Address",
                                "donationType": "Cash donation",
                                "paymentMethod": "Cash"
                                """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("amount"))));
    }

    @Test
    void donationRecordWithNegativeAmountReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/donation-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("""
                                "donorName": "Donor",
                                "dateDonated": "2026-01-01",
                                "donorAddress": "Address",
                                "donationType": "Cash donation",
                                "amount": -0.01,
                                "paymentMethod": "Cash"
                                """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("amount"))));
    }

    @Test
    void donationRecordWithoutPaymentMethodReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/donation-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("""
                                "donorName": "Donor",
                                "dateDonated": "2026-01-01",
                                "donorAddress": "Address",
                                "donationType": "Cash donation",
                                "amount": 10.00
                                """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("paymentMethod"))));
    }

    @Test
    void donationRecordWithInvalidPaymentMethodReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/donation-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("""
                                "donorName": "Donor",
                                "dateDonated": "2026-01-01",
                                "donorAddress": "Address",
                                "donationType": "Cash donation",
                                "amount": 10.00,
                                "paymentMethod": "Bitcoin"
                                """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid value for paymentMethod")));
    }

    @Test
    void donationRecordWithoutDonationTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/donation-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("""
                                "donorName": "Donor",
                                "dateDonated": "2026-01-01",
                                "donorAddress": "Address",
                                "amount": 10.00,
                                "paymentMethod": "Cash"
                                """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("donationType"))));
    }

    @Test
    void announcementWithoutTitleReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Message",
                                  "category": "General",
                                  "status": "Published"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("title"))));
    }

    @Test
    void announcementWithoutContentReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Title",
                                  "category": "General",
                                  "status": "Published"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("message"))));
    }

    @Test
    void announcementWithInvalidStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/announcements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Title",
                                  "message": "Message",
                                  "category": "General",
                                  "status": "Archived"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("status"))));
    }

    @Test
    void userWithoutPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "missing-password",
                                  "email": "missing-password@example.org",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(containsString("password"))));
    }

    @Test
    void userWithWeakPasswordReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "weak-password",
                                  "email": "weak-password@example.org",
                                  "password": "12345",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("at least 6")));
    }

    @Test
    void duplicateUsernameReturnsClientError() throws Exception {
        String body = """
                {
                  "username": "duplicate-user",
                  "email": "duplicate-user@example.org",
                  "password": "secret123",
                  "role": "USER"
                }
                """;
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void malformedJsonReturnsBadRequestWithApiErrorShape() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"))
                .andExpect(jsonPath("$.path").value("/api/goals"));
    }

    @Test
    void unknownGoalIdReturnsNotFoundForReadUpdateAndDelete() throws Exception {
        mockMvc.perform(get("/api/goals/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(put("/api/goals/999999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Missing",
                                  "targetAmount": 100.00,
                                  "currentAmount": 0.00
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(delete("/api/goals/999999").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String recordJson(String fields) {
        return "{" + fields + "}";
    }
}
