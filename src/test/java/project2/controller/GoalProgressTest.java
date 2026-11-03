package project2.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import project2.entity.DonationGoal;
import project2.repository.DonationGoalRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GoalProgressTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DonationGoalRepository goalRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void progressIsCurrentAmountDividedByTargetAmountTimesOneHundred() throws Exception {
        DonationGoal goal = saveGoal("Halfway", "200.00", "50.00");

        JsonNode body = getGoal(goal.getId());
        BigDecimal progress = progress(body.get("currentAmount").decimalValue(),
                body.get("targetAmount").decimalValue());

        assertEquals(0, new BigDecimal("25.00").compareTo(progress));
    }

    @Test
    void zeroTargetAmountProducesZeroProgressWithoutDivisionError() throws Exception {
        DonationGoal goal = saveGoal("No target", "0.00", "25.00");

        JsonNode body = getGoal(goal.getId());
        BigDecimal progress = progress(body.get("currentAmount").decimalValue(),
                body.get("targetAmount").decimalValue());

        assertEquals(0, BigDecimal.ZERO.compareTo(progress));
    }

    private JsonNode getGoal(Long id) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/goals/" + id))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private BigDecimal progress(BigDecimal currentAmount, BigDecimal targetAmount) {
        if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return currentAmount.multiply(new BigDecimal("100"))
                .divide(targetAmount, 2, RoundingMode.HALF_UP);
    }

    private DonationGoal saveGoal(String title, String targetAmount, String currentAmount) {
        DonationGoal goal = new DonationGoal();
        goal.setTitle(title);
        goal.setTargetAmount(new BigDecimal(targetAmount));
        goal.setCurrentAmount(new BigDecimal(currentAmount));
        return goalRepository.save(goal);
    }
}
