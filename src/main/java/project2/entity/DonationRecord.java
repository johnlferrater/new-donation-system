package project2.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.sql.Date;

@Entity
@Table(name = "donation_records")
public class DonationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int recordId;

    @NotBlank(message = "Donor name is required")
    private String donorName;

    @NotNull(message = "Date donated is required")
    @PastOrPresent(message = "Date donated cannot be in the future")
    private Date dateDonated;

    @NotBlank(message = "Donor address is required")
    private String donorAddress;

    @NotBlank(message = "Donation type is required")
    private String donationType;

    @PositiveOrZero(message = "Amount cannot be negative")
    private double amount;

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public Date getDateDonated() { return dateDonated; }
    public void setDateDonated(Date dateDonated) { this.dateDonated = dateDonated; }

    public String getDonorAddress() { return donorAddress; }
    public void setDonorAddress(String donorAddress) { this.donorAddress = donorAddress; }

    public String getDonationType() { return donationType; }
    public void setDonationType(String donationType) { this.donationType = donationType; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
