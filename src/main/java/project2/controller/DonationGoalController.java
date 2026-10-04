package project2.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import project2.entity.DonationGoal;
import project2.exception.ResourceNotFoundException;
import project2.repository.DonationGoalRepository;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class DonationGoalController {

    private final DonationGoalRepository repo;

    public DonationGoalController(DonationGoalRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<DonationGoal> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public DonationGoal getOne(@PathVariable Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DonationGoal", id));
    }

    @PostMapping
    public DonationGoal create(@Valid @RequestBody DonationGoal goal) {
        goal.setId(null);
        if (goal.getCurrentAmount() < 0) {
            goal.setCurrentAmount(0);
        }
        return repo.save(goal);
    }

    @PutMapping("/{id}")
    public DonationGoal update(@PathVariable Long id, @Valid @RequestBody DonationGoal update) {
        DonationGoal existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DonationGoal", id));

        existing.setTitle(update.getTitle());
        existing.setTargetAmount(update.getTargetAmount());
        existing.setCurrentAmount(update.getCurrentAmount());
        return repo.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("DonationGoal", id);
        }
        repo.deleteById(id);
    }
}
