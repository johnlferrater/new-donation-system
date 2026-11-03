package project2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project2.entity.DonationType;
import project2.entity.PaymentMethod;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/donation-options")
public class DonationOptionsController {

    @GetMapping("/types")
    public List<String> getDonationTypes() {
        return DonationType.labels();
    }

    @GetMapping("/payment-methods")
    public List<String> getPaymentMethods() {
        return Arrays.stream(PaymentMethod.values()).map(Enum::name).toList();
    }
}
