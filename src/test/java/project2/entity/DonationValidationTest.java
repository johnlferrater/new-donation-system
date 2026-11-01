package project2.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DonationValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsNegativeRecordAmount() {
        DonationRecord record = new DonationRecord();
        record.setDonorName("Donor");
        record.setDonorAddress("Address");
        record.setDonationType("Cash donation");
        record.setDateDonated(Date.valueOf(LocalDate.now()));
        record.setAmount(new BigDecimal("-0.01"));
        record.setPaymentMethod(PaymentMethod.Cash);

        assertTrue(validator.validate(record).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void rejectsNegativeGoalAmounts() {
        DonationGoal goal = new DonationGoal();
        goal.setTitle("Goal");
        goal.setTargetAmount(new BigDecimal("-0.01"));
        goal.setCurrentAmount(new BigDecimal("-0.01"));

        assertTrue(validator.validate(goal).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("targetAmount")));
        assertTrue(validator.validate(goal).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("currentAmount")));
    }
}
