package project2.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project2.entity.ProfilePicture;
import project2.entity.User;
import project2.exception.ResourceNotFoundException;
import project2.repository.ProfilePictureRepository;
import project2.repository.UserRepository;
import project2.validation.OnCreate;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final ProfilePictureRepository profilePictureRepo;

    public UserController(UserRepository repo, PasswordEncoder passwordEncoder,
                          ProfilePictureRepository profilePictureRepo) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.profilePictureRepo = profilePictureRepo;
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
        if (authentication != null) {
            isSelf = username.equals(authentication.getName());
            if (authentication.getAuthorities() != null) {
                isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a != null && "ROLE_ADMIN".equals(a.getAuthority()));
            }
        } else {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        if (!isAdmin && !isSelf) {
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
            if (!isAdmin) {
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

    @PostMapping("/me/profile-picture")
    @Transactional
    public Map<String, String> uploadProfilePicture(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        String username = authentication.getName();
        User existing = repo.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be 5MB or less");
        }

        byte[] imageData;
        try {
            imageData = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file");
        }

        String contentType = detectImageContentType(imageData);
        if (contentType == null) {
            throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are allowed");
        }

        ProfilePicture picture = profilePictureRepo.findById(username)
                .orElseGet(ProfilePicture::new);
        picture.setUsername(username);
        picture.setImageData(imageData);
        picture.setContentType(contentType);
        picture.setUpdatedAt(Instant.now());
        profilePictureRepo.save(picture);

        existing.setProfilePicture("db");
        repo.save(existing);

        return Map.of("profilePicture", "db", "url", "/api/users/me/profile-picture");
    }

    @GetMapping("/{username}/profile-picture")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable String username, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        String targetUsername = "me".equals(username) ? authentication.getName() : username;
        return profilePictureRepo.findById(targetUsername)
                .map(picture -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, picture.getContentType())
                        .header("X-Content-Type-Options", "nosniff")
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                        .body(picture.getImageData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private String detectImageContentType(byte[] data) {
        if (data == null || data.length < 4) {
            return null;
        }
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if ((data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return "image/png";
        }
        if (data.length >= 12
                && data[0] == 0x52 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x46
                && data[8] == 0x57 && data[9] == 0x45 && data[10] == 0x42 && data[11] == 0x50) {
            return "image/webp";
        }
        return null;
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
        profilePictureRepo.findById(username).ifPresent(profilePictureRepo::delete);
        repo.delete(existing);
    }
}
