package project2.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.sql.Timestamp;

@Entity
@Table(name = "announcements")
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "Message is required")
    private String message;

    @Column(name = "date_posted")
    private Timestamp datePosted;

    @PrePersist
    protected void onCreate() {
        if (datePosted == null) {
            datePosted = new Timestamp(System.currentTimeMillis());
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Timestamp getDatePosted() { return datePosted; }
    public void setDatePosted(Timestamp datePosted) { this.datePosted = datePosted; }
}
