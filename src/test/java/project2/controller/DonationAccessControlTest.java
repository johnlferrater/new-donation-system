package project2.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import project2.entity.DonationHistory;
import project2.entity.DonationRecord;
import project2.entity.PaymentMethod;
import project2.repository.DonationHistoryRepository;
import project2.repository.DonationRecordRepository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DonationAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DonationHistoryRepository historyRepository;

    @Autowired
    private DonationRecordRepository recordRepository;

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void userCannotReadAllDonationRecords() throws Exception {
        mockMvc.perform(get("/api/donation-records"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void anonymousCannotCreateDonationHistory() throws Exception {
        mockMvc.perform(post("/api/donation-history")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson("anonymous", "Item")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void userCanCreateRecordAndMatchingHistoryUsesAuthenticatedUsername() throws Exception {
        mockMvc.perform(post("/api/donation-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("Spoofed Donor")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/donation-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("member"));
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void userSeesOnlyOwnDonationHistoryNewestFirstAndCannotReadAnotherUsersEntry() throws Exception {
        saveHistory("member", "Older", Instant.parse("2026-01-02T10:00:00Z"));
        saveHistory("member", "Newest", Instant.parse("2026-01-03T10:00:00Z"));
        saveHistory("someone-else", "Theirs");
        DonationHistory other = saveHistory("someone-else", "Another");

        mockMvc.perform(get("/api/donation-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("member"))
                .andExpect(jsonPath("$[0].item").value("Newest"));

        mockMvc.perform(get("/api/donation-history/" + other.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void userPostForcesAuthenticatedUsername() throws Exception {
        mockMvc.perform(post("/api/donation-history")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson("spoofed-user", "Food")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("member"));
    }

    @Test
    @WithMockUser(username = "member", roles = "USER")
    void userCannotUpdateOrDeleteRecordsOrHistory() throws Exception {
        DonationRecord record = saveRecord("member");
        DonationHistory history = saveHistory("member", "Item");

        mockMvc.perform(put("/api/donation-records/" + record.getRecordId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("member")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(delete("/api/donation-records/" + record.getRecordId()).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/donation-history/" + history.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson("member", "Updated")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/donation-history/" + history.getId()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanReadUpdateAndDeleteRecords() throws Exception {
        DonationRecord record = saveRecord("Donor");

        mockMvc.perform(get("/api/donation-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(put("/api/donation-records/" + record.getRecordId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordJson("Updated Donor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donorName").value("Updated Donor"));

        mockMvc.perform(delete("/api/donation-records/" + record.getRecordId()).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanReadAllHistoryAndUpdateOrDeleteEntries() throws Exception {
        DonationHistory first = saveHistory("first-user", "First");
        saveHistory("second-user", "Second");

        mockMvc.perform(get("/api/donation-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(put("/api/donation-history/" + first.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson("first-user", "Updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item").value("Updated"));

        mockMvc.perform(delete("/api/donation-history/" + first.getId()).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminPostKeepsSubmittedUsername() throws Exception {
        mockMvc.perform(post("/api/donation-history")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson("chosen-user", "Entry")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("chosen-user"));
    }

    private DonationRecord saveRecord(String donorName) {
        DonationRecord record = new DonationRecord();
        record.setDonorName(donorName);
        record.setDonorAddress("Address");
        record.setDonationType("Cash donation");
        record.setDateDonated(Date.valueOf("2026-01-01"));
        record.setAmount(new BigDecimal("10.00"));
        record.setPaymentMethod(PaymentMethod.Cash);
        return recordRepository.save(record);
    }

    private DonationHistory saveHistory(String username, String item) {
        return saveHistory(username, item, Instant.parse("2026-01-02T10:00:00Z"));
    }

    private DonationHistory saveHistory(String username, String item, Instant donatedAt) {
        DonationHistory history = new DonationHistory();
        history.setUsername(username);
        history.setItem(item);
        history.setAmount(new BigDecimal("10.00"));
        history.setPaymentMethod(PaymentMethod.Cash);
        history.setDateDonated(Timestamp.from(donatedAt));
        return historyRepository.save(history);
    }

    private String recordJson(String donorName) {
        return """
                {
                  "donorName": "%s",
                  "dateDonated": "2026-01-01",
                  "donorAddress": "Address",
                  "donationType": "Cash donation",
                  "amount": 10.00,
                  "paymentMethod": "Cash"
                }
                """.formatted(donorName);
    }

    private String historyJson(String username, String item) {
        return """
                {
                  "username": "%s",
                  "item": "%s",
                  "amount": 10.00,
                  "paymentMethod": "Cash"
                }
                """.formatted(username, item);
    }
}
