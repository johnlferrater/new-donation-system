package project2.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import project2.entity.DonationHistory;
import project2.exception.ResourceNotFoundException;
import project2.repository.DonationHistoryRepository;
import project2.service.DonationSyncService;

import java.util.List;

@RestController
@RequestMapping("/api/donation-history")
public class DonationHistoryController {

    private final DonationHistoryRepository repo;
    private final DonationSyncService syncService;

    public DonationHistoryController(DonationHistoryRepository repo, DonationSyncService syncService) {
        this.repo = repo;
        this.syncService = syncService;
    }

    @GetMapping
    public List<DonationHistory> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public DonationHistory getOne(@PathVariable Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DonationHistory", id));
    }

    @PostMapping
    public DonationHistory create(@Valid @RequestBody DonationHistory history) {
        // Also writes a matching entry into donation records.
        return syncService.saveHistoryWithRecord(history);
    }

    @PutMapping("/{id}")
    public DonationHistory update(@PathVariable Long id, @Valid @RequestBody DonationHistory update) {
        DonationHistory existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DonationHistory", id));

        existing.setUsername(update.getUsername());
        existing.setItem(update.getItem());
        existing.setAmount(update.getAmount());
        existing.setPaymentMethod(update.getPaymentMethod());
        return repo.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("DonationHistory", id);
        }
        repo.deleteById(id);
    }
}
