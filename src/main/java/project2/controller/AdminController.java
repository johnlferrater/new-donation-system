package project2.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import project2.entity.Admin;
import project2.repository.AdminRepository;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
@CrossOrigin
public class AdminController {
    @Autowired
    private AdminRepository repo;

    @GetMapping
    public List<Admin> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public Admin create(@RequestBody Admin admin) {
        return repo.save(admin);
    }
}
