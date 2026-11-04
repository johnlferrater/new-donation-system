package project2.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import project2.entity.User;
import project2.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Pattern;

@Controller
public class RegisterController {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminSignupCode;

    public RegisterController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                              @Value("${app.admin-signup-code:}") String adminSignupCode) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminSignupCode = adminSignupCode == null ? "" : adminSignupCode;
    }

    @GetMapping("/register")
    public String showForm(Model model) {
        addRoleModelAttributes(model, false);
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam(required = false) String role,
                           @RequestParam(required = false) String adminCode,
                           Model model) {
        username = username.trim();
        email = email.trim();
        model.addAttribute("username", username);
        model.addAttribute("email", email);

        boolean adminRequested = "ADMIN".equals(role == null ? "" : role.trim().toUpperCase(Locale.ROOT));
        addRoleModelAttributes(model, adminRequested);

        String error = null;
        if (!USERNAME.matcher(username).matches()) {
            error = "Username must be 3-50 characters: letters, numbers, dot, dash or underscore.";
        } else if (!EMAIL.matcher(email).matches()) {
            error = "Please enter a valid email address.";
        } else if (password.length() < 6) {
            error = "Password must be at least 6 characters.";
        } else if (!password.equals(confirmPassword)) {
            error = "Passwords do not match.";
        } else if (adminRequested && !isValidAdminCode(adminCode)) {
            error = "Invalid admin code";
        } else if (userRepository.existsById(username)) {
            error = "That username is already taken.";
        } else if (userRepository.existsByEmail(email)) {
            error = "That email is already in use.";
        }

        if (error != null) {
            model.addAttribute("error", error);
            return "register";
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        // Role is decided here, never taken directly from the submitted value.
        user.setRole(adminRequested ? "ADMIN" : "USER");
        userRepository.save(user);

        return "redirect:/login?registered";
    }

    private void addRoleModelAttributes(Model model, boolean adminRequested) {
        model.addAttribute("adminSignupEnabled", isAdminSignupEnabled());
        model.addAttribute("selectedRole", adminRequested ? "ADMIN" : "USER");
    }

    private boolean isAdminSignupEnabled() {
        return !adminSignupCode.isBlank();
    }

    private boolean isValidAdminCode(String submittedCode) {
        if (!isAdminSignupEnabled() || submittedCode == null) {
            return false;
        }
        return MessageDigest.isEqual(
                adminSignupCode.getBytes(StandardCharsets.UTF_8),
                submittedCode.getBytes(StandardCharsets.UTF_8));
    }
}
