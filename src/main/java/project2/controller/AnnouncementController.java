package project2.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project2.entity.Announcement;
import project2.exception.ResourceNotFoundException;
import project2.repository.AnnouncementRepository;

import java.sql.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementRepository repo;

    public AnnouncementController(AnnouncementRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Announcement> getAll(Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return repo.findAllByOrderByCreatedAtDescDatePostedDesc();
        }
        return repo.findAllByPublishedTrueOrderByPublishedAtDescDatePostedDesc();
    }

    @GetMapping("/{id}")
    public Announcement getOne(@PathVariable Long id, Authentication authentication) {
        Announcement announcement = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && !announcement.isPublished()) {
            throw new ResourceNotFoundException("Announcement", id);
        }
        return announcement;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Announcement create(@Valid @RequestBody Announcement announcement, Authentication authentication) {
        announcement.setId(null);
        if (announcement.getCreatedBy() == null && authentication != null) {
            announcement.setCreatedBy(authentication.getName());
        }
        if (announcement.getMessage() == null && announcement.getAnnouncementContent() != null) {
            announcement.setMessage(announcement.getAnnouncementContent());
        }
        if (announcement.getAnnouncementContent() == null && announcement.getMessage() != null) {
            announcement.setAnnouncementContent(announcement.getMessage());
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (announcement.getCreatedAt() == null) {
            announcement.setCreatedAt(now);
        }
        if (announcement.getUpdatedAt() == null) {
            announcement.setUpdatedAt(now);
        }
        if (announcement.getDatePosted() == null) {
            announcement.setDatePosted(now);
        }
        if (announcement.getStatus() != null) {
            if ("Published".equalsIgnoreCase(announcement.getStatus())) {
                announcement.setPublished(true);
            } else if ("Draft".equalsIgnoreCase(announcement.getStatus())) {
                announcement.setPublished(false);
            }
        }
        if (announcement.isPublished()) {
            if (announcement.getPublishedAt() == null) {
                announcement.setPublishedAt(now);
            }
        } else {
            announcement.setPublishedAt(null);
        }
        if (announcement.getTitle() == null || announcement.getTitle().isBlank()) {
            String msg = announcement.getMessage() != null ? announcement.getMessage() : announcement.getAnnouncementContent();
            announcement.setTitle(msg != null && msg.length() > 50 ? msg.substring(0, 50) : (msg == null ? "" : msg));
        }
        if (announcement.getStatus() == null) {
            announcement.setStatus(announcement.isPublished() ? "Published" : "Draft");
        }
        return repo.save(announcement);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Announcement update(@PathVariable Long id, @Valid @RequestBody Announcement update) {
        Announcement existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));

        if (update.getTitle() != null) {
            existing.setTitle(update.getTitle());
        }
        if (update.getMessage() != null) {
            existing.setMessage(update.getMessage());
        }
        if (update.getAnnouncementContent() != null) {
            existing.setAnnouncementContent(update.getAnnouncementContent());
            if (existing.getMessage() == null) {
                existing.setMessage(update.getAnnouncementContent());
            }
        }
        if (update.getCategory() != null) {
            existing.setCategory(update.getCategory());
        }
        if (update.getStatus() != null) {
            existing.setStatus(update.getStatus());
        } else {
            existing.setPublished(update.isPublished());
        }
        if (update.getPublicationDate() != null) {
            existing.setPublicationDate(update.getPublicationDate());
        }
        if (update.getExpirationDate() != null) {
            existing.setExpirationDate(update.getExpirationDate());
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        existing.setUpdatedAt(now);
        if (update.getPublishedAt() != null) {
            existing.setPublishedAt(update.getPublishedAt());
        } else if (existing.isPublished() && existing.getPublishedAt() == null) {
            existing.setPublishedAt(now);
        } else if (!existing.isPublished()) {
            existing.setPublishedAt(null);
        }
        if (existing.getDatePosted() == null) {
            existing.setDatePosted(now);
        }
        if (existing.getTitle() == null || existing.getTitle().isBlank()) {
            String msg = existing.getMessage() != null ? existing.getMessage() : existing.getAnnouncementContent();
            existing.setTitle(msg != null && msg.length() > 50 ? msg.substring(0, 50) : (msg == null ? "" : msg));
        }
        if (existing.getStatus() == null) {
            existing.setStatus(existing.isPublished() ? "Published" : "Draft");
        }
        return repo.save(existing);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public Announcement publish(@PathVariable Long id) {
        Announcement existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
        Timestamp now = new Timestamp(System.currentTimeMillis());
        existing.setPublished(true);
        if (existing.getPublishedAt() == null) {
            existing.setPublishedAt(now);
        }
        existing.setUpdatedAt(now);
        return repo.save(existing);
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    public Announcement unpublish(@PathVariable Long id) {
        Announcement existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
        Timestamp now = new Timestamp(System.currentTimeMillis());
        existing.setPublished(false);
        existing.setPublishedAt(null);
        existing.setUpdatedAt(now);
        return repo.save(existing);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Announcement", id);
        }
        repo.deleteById(id);
    }
}
