package project2.controller;

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
        return "admins";
    }

    @GetMapping("/goals")
    public String goals() {
        return "goals";
    }

    @GetMapping("/donation-records")
    public String donationRecords() {
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

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
