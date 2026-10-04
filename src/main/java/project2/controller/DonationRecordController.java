package project2.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import project2.entity.DonationRecord;
import project2.exception.ResourceNotFoundException;
import project2.repository.DonationRecordRepository;
import project2.service.DonationSyncService;

import java.util.List;

@RestController
@RequestMapping("/api/donation-records")
public class DonationRecordController {

    private final DonationRecordRepository repo;
    private final DonationSyncService syncService;

    public DonationRecordController(DonationRecordRepository repo, DonationSyncService syncService) {
        this.repo = repo;
        this.syncService = syncService;
    }

    @GetMapping
    public List<DonationRecord> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public DonationRecord getOne(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DonationRecord", id));
    }

    @PostMapping
    public DonationRecord create(@Valid @RequestBody DonationRecord record) {
        // Also writes a matching entry into donation history.
        return syncService.saveRecordWithHistory(record);
    }

    @PutMapping("/{id}")
    public DonationRecord update(@PathVariable Integer id, @Valid @RequestBody DonationRecord update) {
        DonationRecord existing = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DonationRecord", id));

        existing.setDonorName(update.getDonorName());
        existing.setDonorAddress(update.getDonorAddress());
        existing.setDonationType(update.getDonationType());
        existing.setDateDonated(update.getDateDonated());
        existing.setAmount(update.getAmount());
        existing.setPaymentMethod(update.getPaymentMethod());
        return repo.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("DonationRecord", id);
        }
        repo.deleteById(id);
    }
}
