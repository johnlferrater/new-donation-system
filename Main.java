package project2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Project2Application {
    public static void main(String[] args) {
        SpringApplication.run(Project2Application.class, args);
    }
}
package project2.model;

import jakarta.persistence.*;
        import java.sql.Timestamp;

@Entity
@Table(name = "users")
public class User {
    @Id
    private String username;

    private String email;
    private String password;

    @Column(name = "date_created")
    private Timestamp dateCreated;

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Timestamp getDateCreated() { return dateCreated; }
    public void setDateCreated(Timestamp dateCreated) { this.dateCreated = dateCreated; }
}
package project2.controller;

import org.springframework.web.bind.annotation.*;
        import org.springframework.beans.factory.annotation.Autowired;
import project2.model.User;
import project2.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/users")
@CrossOrigin
public class UserController {
    @Autowired
    private UserRepository repo;

    @GetMapping
    public List<User> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return repo.save(user);
    }
}
package project2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.model.User;

public interface UserRepository extends JpaRepository<User, String> {}
package project2.model;

import jakarta.persistence.*;
        import java.sql.Date;

@Entity
@Table(name = "donation_records")
public class DonationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private int recordId;

    private String donorName;
    private Date dateDonated;
    private String donorAddress;
    private String donationType;

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
}
package project2.model;

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

    // Getters and setters
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
package project2.model;

import jakarta.persistence.*;
        import java.sql.Timestamp;

@Entity
@Table(name = "announcements")
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    @Column(name = "date_posted")
    private Timestamp datePosted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Timestamp getDatePosted() { return datePosted; }
    public void setDatePosted(Timestamp datePosted) { this.datePosted = datePosted; }
}
package project2.model;

import jakarta.persistence.*;

@Entity
@Table(name = "admins")
public class Admin {
    @Id
    private String admin;
    private String password;

    public String getAdmin() { return admin; }
    public void setAdmin(String admin) { this.admin = admin; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}