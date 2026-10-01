package project2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import project2.entity.DonationRecord;
import project2.repository.DonationRecordRepository;

import java.util.List;

@RestController
@RequestMapping("/api/donation-records")
@CrossOrigin
public class DonationRecordController {

    @Autowired
    private DonationRecordRepository repo;

    @GetMapping
    public List<DonationRecord> getAll() {
        return repo.findAll();
    }

    // Keep your other methods here
}