package project2.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import project2.entity.Announcement;
import project2.repository.AnnouncementRepository;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin
public class AnnouncementController {
    @Autowired
    private AnnouncementRepository repo;

    @GetMapping
    public List<Announcement> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public Announcement create(@RequestBody Announcement announcement) {
        return repo.save(announcement);
    }
}
