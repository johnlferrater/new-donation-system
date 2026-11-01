package project2.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/users")
    public String users() {
        return "users";
    }

    @GetMapping("/admins")
    public String admins() {
        return "redirect:/users";
    }

    @GetMapping("/goals")
    public String goals() {
        return "goals";
    }

    @GetMapping("/donation-records")
    public String donationRecords(Authentication authentication) {
        if (!hasRole(authentication, "ROLE_ADMIN")) {
            return "redirect:/dashboard";
        }
        return "donation-records";
    }

    @GetMapping("/donation-history")
    public String donationHistory() {
        return "donation-history";
    }

    @GetMapping("/announcements")
    public String announcements() {
        return "announcements";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority != null && role.equals(authority.getAuthority()));
    }
}
