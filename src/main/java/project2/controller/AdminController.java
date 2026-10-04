package project2.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import project2.entity.Admin;
import project2.exception.ResourceNotFoundException;
import project2.repository.AdminRepository;
import project2.validation.OnCreate;
import project2.validation.OnUpdate;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    private final AdminRepository repo;
    private final PasswordEncoder passwordEncoder;

    public AdminController(AdminRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Admin> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{name}")
    public Admin getOne(@PathVariable String name) {
        return repo.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", name));
    }

    @PostMapping
    public Admin create(@Validated(OnCreate.class) @RequestBody Admin admin) {
        if (repo.existsById(admin.getAdmin())) {
            throw new IllegalArgumentException("Admin already exists: " + admin.getAdmin());
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return repo.save(admin);
    }

    @PutMapping("/{name}")
    public Admin update(@PathVariable String name,
                        @Validated(OnUpdate.class) @RequestBody Admin update) {
        Admin existing = repo.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", name));

        if (update.getPassword() != null && !update.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(update.getPassword()));
        }
        return repo.save(existing);
    }

    @DeleteMapping("/{name}")
    public void delete(@PathVariable String name) {
        if (!repo.existsById(name)) {
            throw new ResourceNotFoundException("Admin", name);
        }
        repo.deleteById(name);
    }
}
