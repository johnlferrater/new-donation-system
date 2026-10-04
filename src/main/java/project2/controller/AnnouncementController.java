package project2.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import project2.entity.Announcement;
import project2.exception.ResourceNotFoundException;
import project2.repository.AnnouncementRepository;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementRepository repo;

    public AnnouncementController(AnnouncementRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Announcement> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Announcement getOne(@PathVariable Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
    }

    @PostMapping
    public Announcement create(@Valid @RequestBody Announcement announcement) {
        announcement.setId(null);
        return repo.save(announcement);
    }

    @PutMapping("/{id}")
    public Announcement update(@PathVariable Long id, @Valid @RequestBody Announcement update) {
        Announcement existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));

        existing.setMessage(update.getMessage());
        return repo.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Announcement", id);
        }
        repo.deleteById(id);
    }
}
