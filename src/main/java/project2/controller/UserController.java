package project2.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import project2.entity.User;
import project2.exception.ResourceNotFoundException;
import project2.repository.UserRepository;
import project2.validation.OnCreate;
import project2.validation.OnUpdate;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<User> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{username}")
    public User getOne(@PathVariable String username) {
        return repo.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    @PostMapping
    public User create(@Validated(OnCreate.class) @RequestBody User user) {
        if (repo.existsById(user.getUsername())) {
            throw new IllegalArgumentException("User already exists: " + user.getUsername());
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }
        return repo.save(user);
    }

    @PutMapping("/{username}")
    public User update(@PathVariable String username,
                       @Validated(OnUpdate.class) @RequestBody User update) {
        User existing = repo.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        existing.setEmail(update.getEmail());
        if (update.getRole() != null && !update.getRole().isBlank()) {
            existing.setRole(update.getRole());
        }
        if (update.getPassword() != null && !update.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(update.getPassword()));
        }
        return repo.save(existing);
    }

    @DeleteMapping("/{username}")
    public void delete(@PathVariable String username, Authentication authentication) {
        if (authentication != null && username.equals(authentication.getName())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        if (!repo.existsById(username)) {
            throw new ResourceNotFoundException("User", username);
        }
        repo.deleteById(username);
    }
}
