package project2.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @PreAuthorize("hasRole('ADMIN')")
    public List<DonationRecord> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DonationRecord getOne(@PathVariable Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DonationRecord", id));
    }

    @PostMapping
    public DonationRecord create(@Valid @RequestBody DonationRecord record, Authentication authentication) {
        // Also writes a matching entry into donation history.
        String actorUsername = authentication != null ? authentication.getName() : null;
        String historyUsername = null;
        if (authentication != null && !isAdmin(authentication)) {
            historyUsername = actorUsername;
        }
        return syncService.saveRecordWithHistory(record, historyUsername, actorUsername);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Integer id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("DonationRecord", id);
        }
        repo.deleteById(id);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority != null && "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
