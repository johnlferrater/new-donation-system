package project2.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import project2.model.DonationGoal;
import project2.repository.DonationGoalRepository;

import java.util.List;

@RestController
@RequestMapping("/goals")
@CrossOrigin
public class DonationGoalController {
    @Autowired
    private DonationGoalRepository repo;

    @GetMapping
    public List<DonationGoal> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public DonationGoal create(@RequestBody DonationGoal goal) {
        return repo.save(goal);
    }
}
