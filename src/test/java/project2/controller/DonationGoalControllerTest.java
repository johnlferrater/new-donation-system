package project2.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project2.entity.DonationGoal;
import project2.repository.DonationGoalRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonationGoalControllerTest {

    @Mock
    private DonationGoalRepository repository;

    private DonationGoalController controller;

    @BeforeEach
    void setUp() {
        controller = new DonationGoalController(repository);
    }

    @Test
    void keepsExactDecimalAmounts() {
        DonationGoal goal = new DonationGoal();
        goal.setTitle("Exact decimal goal");
        goal.setTargetAmount(new BigDecimal("10.00"));
        goal.setCurrentAmount(new BigDecimal("0.10").add(new BigDecimal("0.20")));
        when(repository.save(any(DonationGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DonationGoal saved = controller.create(goal);

        assertEquals(new BigDecimal("0.30"), saved.getCurrentAmount());
    }

    @Test
    void amountsSerializeAsJsonNumbers() throws Exception {
        DonationGoal goal = new DonationGoal();
        goal.setTargetAmount(new BigDecimal("10.00"));
        goal.setCurrentAmount(new BigDecimal("0.30"));

        JsonNode json = new ObjectMapper().valueToTree(goal);

        assertTrue(json.get("targetAmount").isNumber());
        assertTrue(json.get("currentAmount").isNumber());
        assertEquals(0, new BigDecimal("10.00").compareTo(json.get("targetAmount").decimalValue()));
        assertEquals(0, new BigDecimal("0.30").compareTo(json.get("currentAmount").decimalValue()));
    }
}
