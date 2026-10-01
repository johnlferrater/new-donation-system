package project2.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "donation_history")
public class DonationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String item;
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "date_donated")
    private Timestamp dateDonated;

    public enum PaymentMethod {
        PayMaya, GCash, Personal
    }

    @PrePersist
    protected void onCreate() {
        if (dateDonated == null) {
            dateDonated = new Timestamp(System.currentTimeMillis());
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Timestamp getDateDonated() { return dateDonated; }
    public void setDateDonated(Timestamp dateDonated) { this.dateDonated = dateDonated; }
}
