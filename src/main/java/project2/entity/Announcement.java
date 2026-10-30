package project2.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.sql.Timestamp;
import java.sql.Date;

@Entity
@Table(name = "announcements")
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be at most 200 characters")
    private String title;

    @NotBlank(message = "Message is required")
    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "announcement_content", columnDefinition = "TEXT")
    private String announcementContent;

    @Size(max = 100, message = "Category must be at most 100 characters")
    private String category;

    @Column(name = "publication_date")
    private Date publicationDate;

    @Column(name = "expiration_date")
    private Date expirationDate;

    private String status;

    @Column(name = "date_posted")
    private Timestamp datePosted;

    @Column(name = "published_at")
    private Timestamp publishedAt;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    private boolean published = true;

    @PrePersist
    protected void onCreate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (datePosted == null) {
            datePosted = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (announcementContent == null && message != null) {
            announcementContent = message;
        }
        if (message == null && announcementContent != null) {
            message = announcementContent;
        }
        if (published && publishedAt == null) {
            publishedAt = now;
        }
        if (title == null || title.isBlank()) {
            String source = message != null ? message : announcementContent;
            title = source != null && source.length() > 50 ? source.substring(0, 50) : (source == null ? "" : source);
        }
        if (status == null) {
            status = published ? "Published" : "Draft";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
        if (announcementContent == null && message != null) {
            announcementContent = message;
        }
        if (message == null && announcementContent != null) {
            message = announcementContent;
        }
        if (published && publishedAt == null) {
            publishedAt = updatedAt;
        }
        if (status == null) {
            status = published ? "Published" : "Draft";
        } else if ("Published".equalsIgnoreCase(status) && !published) {
            published = true;
        } else if ("Draft".equalsIgnoreCase(status) && published) {
            // keep as is if explicitly set, but don't force
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) {
        this.message = message;
        if (this.announcementContent == null) {
            this.announcementContent = message;
        }
    }

    public String getAnnouncementContent() { return announcementContent; }
    public void setAnnouncementContent(String announcementContent) {
        this.announcementContent = announcementContent;
        if (this.message == null) {
            this.message = announcementContent;
        }
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Date getPublicationDate() { return publicationDate; }
    public void setPublicationDate(Date publicationDate) { this.publicationDate = publicationDate; }

    public Date getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Date expirationDate) { this.expirationDate = expirationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        if (status != null) {
            if ("Published".equalsIgnoreCase(status)) {
                this.published = true;
            } else if ("Draft".equalsIgnoreCase(status)) {
                this.published = false;
            }
        }
    }

    public Timestamp getDatePosted() { return datePosted; }
    public void setDatePosted(Timestamp datePosted) { this.datePosted = datePosted; }

    public Timestamp getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Timestamp publishedAt) { this.publishedAt = publishedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
