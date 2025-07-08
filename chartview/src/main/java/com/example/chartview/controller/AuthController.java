package com.example.chartview.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> payload, HttpSession session) {
        String username = payload.get("username");
        String password = payload.get("password");

        try {
            String sql = "SELECT password FROM users WHERE username = ?";
            String dbPassword = jdbcTemplate.queryForObject(sql, new Object[]{username}, String.class);

            if (password.equals(dbPassword)) {
                session.setAttribute("user", username);
                return Map.of("success", true, "message", "Login successful");
            }
        } catch (Exception ignored) {}

        return Map.of("success", false, "message", "Invalid credentials");
    }

    @PostMapping("/signup")
    public Map<String, Object> signup(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        try {
            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            int rows = jdbcTemplate.update(sql, username, password);

            if (rows > 0) {
                return Map.of("success", true, "message", "User created successfully");
            }
        } catch (Exception e) {
            return Map.of("success", false, "message", "Username may already exist");
        }

        return Map.of("success", false, "message", "Failed to create user");
    }

    @GetMapping("/check")
    public Map<String, Object> checkSession(HttpSession session) {
        Object user = session.getAttribute("user");
        return Map.of("loggedIn", user != null);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return Map.of("success", true);
    }
}
