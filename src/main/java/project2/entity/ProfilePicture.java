package project2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "profile_pictures")
public class ProfilePicture {

    @Id
    @Column(name = "username", length = 50, nullable = false)
    private String username;

    @Lob
    @Column(name = "image_data", columnDefinition = "MEDIUMBLOB", nullable = false)
    private byte[] imageData;

    @Column(name = "content_type", length = 50, nullable = false)
    private String contentType;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
