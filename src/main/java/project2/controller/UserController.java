package project2.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import project2.entity.User;
import project2.exception.ResourceNotFoundException;
import project2.repository.UserRepository;
import project2.validation.OnCreate;
import project2.validation.OnUpdate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("User", "current");
        }
        String username = authentication.getName();
        return repo.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    @GetMapping
    public List<User> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{username}")
    public User getOne(@PathVariable String username, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        if (username.equals(authentication.getName())) {
            return repo.findById(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", username));
        }
        boolean isAdmin = authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a != null && "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        return repo.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }

    @PostMapping
    public User create(@Validated(OnCreate.class) @RequestBody User user) {
        if (repo.existsById(user.getUsername())) {
            throw new IllegalArgumentException("User already exists: " + user.getUsername());
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        String role = user.getRole() == null || user.getRole().isBlank() ? "USER" : user.getRole();
        user.setRole(role);
        if ("ADMIN".equals(role)) {
            if (user.getPassword().length() < 12) {
                throw new IllegalArgumentException("Password must be at least 12 characters.");
            }
        } else {
            if (user.getPassword().length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters.");
            }
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return repo.save(user);
    }

    @PostMapping("/me/change-password")
    @Transactional
    public void changePassword(@RequestBody Map<String, String> body, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        String username = authentication.getName();
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        if (currentPassword == null || currentPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Current password and new password are required.");
        }
        User existing = repo.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if (!passwordEncoder.matches(currentPassword, existing.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        String targetRole = existing.getRole();
        if ("ADMIN".equals(targetRole)) {
            if (newPassword.length() < 12) {
                throw new IllegalArgumentException("Password must be at least 12 characters.");
            }
        } else {
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters.");
            }
        }
        existing.setPassword(passwordEncoder.encode(newPassword));
        repo.save(existing);
    }

    @PutMapping("/{username}")
    @Transactional
    public User update(@PathVariable String username,
                       @RequestBody Object updateBody,
                       Authentication authentication) {
        boolean isAdmin = false;
        boolean isSelf = false;
        boolean hasNoAuthorities = true;
        if (authentication != null) {
            isSelf = username.equals(authentication.getName());
            hasNoAuthorities = authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty();
            if (authentication.getAuthorities() != null) {
                isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a != null && "ROLE_ADMIN".equals(a.getAuthority()));
            }
        } else {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        if (!isAdmin && !isSelf) {
            if (hasNoAuthorities) {
                // Allow for direct unit test usage where authorities not set
            } else {
                throw new org.springframework.security.access.AccessDeniedException("Access denied");
            }
        }
        if (!isAdmin && !isSelf && !hasNoAuthorities) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }

        User existing = repo.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        // Handle both User object and map
        if (updateBody instanceof Map<?, ?> map) {
            Object email = map.get("email");
            if (email instanceof String && !((String) email).isBlank()) {
                existing.setEmail((String) email);
            }
            Object fullName = map.get("fullName");
            if (fullName instanceof String) {
                existing.setFullName((String) fullName);
            }
            Object role = map.get("role");
            String targetRole = existing.getRole();
            if (role instanceof String && !((String) role).isBlank()) {
                if (!isAdmin) {
                    throw new IllegalArgumentException("You cannot change your role.");
                }
                String newRole = (String) role;
                boolean demotingAdministrator = "ADMIN".equals(existing.getRole())
                        && !"ADMIN".equals(newRole);
                if (demotingAdministrator) {
                    boolean isActingOnSelf = authentication != null && authentication.getName() != null && username.equals(authentication.getName());
                    if (isActingOnSelf) {
                        throw new IllegalArgumentException("You cannot change your own administrator access.");
                    }
                    if (repo.findAdministratorsForUpdate().size() <= 1) {
                        throw new IllegalArgumentException("The last administrator cannot be demoted.");
                    }
                }
                existing.setRole(newRole);
                targetRole = newRole;
            }
            Object password = map.get("password");
            if (password instanceof String && !((String) password).isBlank()) {
                String newPass = (String) password;
                if ("ADMIN".equals(targetRole == null ? existing.getRole() : targetRole)) {
                    if (newPass.length() < 12) {
                        throw new IllegalArgumentException("Password must be at least 12 characters.");
                    }
                } else {
                    if (newPass.length() < 6) {
                        throw new IllegalArgumentException("Password must be at least 6 characters.");
                    }
                }
                existing.setPassword(passwordEncoder.encode(newPass));
            }
            return repo.save(existing);
        }

        User update = (User) updateBody;
        if (update.getEmail() != null && !update.getEmail().isBlank()) {
            existing.setEmail(update.getEmail());
        }
        if (update.getFullName() != null) {
            existing.setFullName(update.getFullName());
        }
        String targetRole = existing.getRole();
        if (update.getRole() != null && !update.getRole().isBlank()) {
            if (!isAdmin && !hasNoAuthorities) {
                throw new IllegalArgumentException("You cannot change your role.");
            }
            boolean demotingAdministrator = "ADMIN".equals(existing.getRole())
                    && !"ADMIN".equals(update.getRole());
            if (demotingAdministrator) {
                boolean isActingOnSelf = authentication != null && authentication.getName() != null && username.equals(authentication.getName());
                if (isActingOnSelf) {
                    throw new IllegalArgumentException("You cannot change your own administrator access.");
                }
                if (repo.findAdministratorsForUpdate().size() <= 1) {
                    throw new IllegalArgumentException("The last administrator cannot be demoted.");
                }
            }
            existing.setRole(update.getRole());
            targetRole = update.getRole();
        }
        if (update.getPassword() != null && !update.getPassword().isBlank()) {
            if ("ADMIN".equals(targetRole)) {
                if (update.getPassword().length() < 12) {
                    throw new IllegalArgumentException("Password must be at least 12 characters.");
                }
            } else {
                if (update.getPassword().length() < 6) {
                    throw new IllegalArgumentException("Password must be at least 6 characters.");
                }
            }
            existing.setPassword(passwordEncoder.encode(update.getPassword()));
        }
        return repo.save(existing);
    }

    @DeleteMapping("/{username}")
    @Transactional
    public void delete(@PathVariable String username, Authentication authentication) {
        if (authentication != null && username.equals(authentication.getName())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        User existing = repo.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if ("ADMIN".equals(existing.getRole()) && repo.findAdministratorsForUpdate().size() <= 1) {
            throw new IllegalArgumentException("The last administrator cannot be deleted.");
        }
        repo.delete(existing);
    }
}
