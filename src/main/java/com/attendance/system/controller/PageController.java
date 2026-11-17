package com.attendance.system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
public class PageController {

    private void addUser(Model model, String page) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("page", page);
        if (auth != null && auth.isAuthenticated()) {
            model.addAttribute("user", auth.getName());
            String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse(auth.getAuthorities().iterator().next().getAuthority());
            model.addAttribute("role", role);
            model.addAttribute("isAdmin", "ADMIN".equals(role));
        }
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        addUser(model, "dashboard");
        return "dashboard";
    }

    @GetMapping("/students")
    public String students(Model model) {
        addUser(model, "students");
        return "students";
    }

    @GetMapping("/courses")
    public String courses(Model model) {
        addUser(model, "courses");
        return "courses";
    }

    @GetMapping("/subjects")
    public String subjects(Model model) {
        addUser(model, "subjects");
        return "subjects";
    }

    @GetMapping("/attendance")
    public String attendance(Model model) {
        addUser(model, "attendance");
        return "attendance";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        addUser(model, "reports");
        return "reports";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        addUser(model, "profile");
        return "profile";
    }

    @GetMapping("/users")
    public String users(Model model) {
        addUser(model, "users");
        return "users";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        addUser(model, "settings");
        return "settings";
    }

    @GetMapping("/403")
    public String forbidden(Model model) {
        addUser(model, "403");
        return "403";
    }
}