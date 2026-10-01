package project2.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import project2.entity.DonationHistory;
import project2.repository.DonationHistoryRepository;

import java.util.List;

@RestController
@RequestMapping("/api/donation-history")
@CrossOrigin
public class DonationHistoryController {
    @Autowired
    private DonationHistoryRepository repo;

    @GetMapping
    public List<DonationHistory> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public DonationHistory create(@RequestBody DonationHistory history) {
        return repo.save(history);
    }
}
