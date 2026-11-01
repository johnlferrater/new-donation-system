package project2.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DonationApiErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void invalidPaymentMethodReturnsReadableBadRequest() throws Exception {
        mockMvc.perform(post("/api/donation-records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "donorName": "Donor",
                                  "dateDonated": "2026-01-01",
                                  "donorAddress": "Address",
                                  "donationType": "Cash donation",
                                  "amount": 100.00,
                                  "paymentMethod": "Bitcoin"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid value for paymentMethod")))
                .andExpect(jsonPath("$.message", containsString("Cash")))
                .andExpect(jsonPath("$.message", containsString("CebuanaLhuillier")));
    }
}
