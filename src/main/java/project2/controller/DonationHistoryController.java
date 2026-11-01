package project2.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    public List<DonationHistory> getAll(Authentication authentication) {
        if (isAdmin(authentication)) {
            return repo.findAll();
        }
        return repo.findAllByUsernameOrderByDateDonatedDesc(requireUsername(authentication));
    }

    @GetMapping("/{id}")
    public DonationHistory getOne(@PathVariable Long id, Authentication authentication) {
        DonationHistory history = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DonationHistory", id));
        if (!isAdmin(authentication) && !history.getUsername().equals(requireUsername(authentication))) {
            throw new ResourceNotFoundException("DonationHistory", id);
        }
        return history;
    }

    @PostMapping
    public DonationHistory create(@Valid @RequestBody DonationHistory history, Authentication authentication) {
        if (!isAdmin(authentication)) {
            history.setUsername(requireUsername(authentication));
        }
        // Also writes a matching entry into donation records.
        return syncService.saveHistoryWithRecord(history);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("DonationHistory", id);
        }
        repo.deleteById(id);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority != null && "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private String requireUsername(Authentication authentication) {
        if (authentication == null || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Access denied");
        }
        return authentication.getName();
    }
}
