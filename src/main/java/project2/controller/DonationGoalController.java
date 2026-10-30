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

        if (update.getTitle() != null) {
            existing.setTitle(update.getTitle());
        }
        if (update.getDescription() != null) {
            existing.setDescription(update.getDescription());
        }
        existing.setTargetAmount(update.getTargetAmount());
        if (update.getCurrentAmount() >= 0) {
            existing.setCurrentAmount(update.getCurrentAmount());
        }
        existing.setStartDate(update.getStartDate());
        existing.setEndDate(update.getEndDate());
        if (update.getStatus() != null) {
            existing.setStatus(update.getStatus());
        }
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
