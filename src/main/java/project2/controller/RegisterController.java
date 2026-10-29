package project2.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import project2.entity.User;
import project2.repository.UserRepository;

import java.util.regex.Pattern;

@Controller
public class RegisterController {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           Model model) {
        username = username.trim();
        email = email.trim();
        model.addAttribute("username", username);
        model.addAttribute("email", email);

        String error = null;
        if (!USERNAME.matcher(username).matches()) {
            error = "Username must be 3-50 characters: letters, numbers, dot, dash or underscore.";
        } else if (!EMAIL.matcher(email).matches()) {
            error = "Please enter a valid email address.";
        } else if (password.length() < 6) {
            error = "Password must be at least 6 characters.";
        } else if (!password.equals(confirmPassword)) {
            error = "Passwords do not match.";
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
        user.setRole("USER"); // never taken from the form
        userRepository.save(user);

        return "redirect:/login?registered";
    }
}