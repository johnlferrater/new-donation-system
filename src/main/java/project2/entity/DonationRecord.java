package project2.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
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

    @DecimalMin(value = "0.00", message = "Amount cannot be negative")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
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

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
