package ca.group13.codecomparator.controller;

import ca.group13.codecomparator.dto.LoginRequest;
import ca.group13.codecomparator.dto.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/* Handles login for instructors. Checks credentials stored in DB to confirm a valid user
 * is trying to access the system. */

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final JdbcTemplate jdbc;

    public ApiAuthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {

        String email = (req == null || req.getEmail() == null) ? "" : req.getEmail().trim();
        String password = (req == null || req.getPassword() == null) ? "" : req.getPassword().trim();

        if (email.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing email or password");
        }

        Map<String, Object> row = jdbc.query("""
                SELECT password_hash, role
                FROM users
                WHERE user_email_enc = ?
                LIMIT 1
                """, rs -> {
            if (!rs.next()) return null;
            return Map.<String, Object>of("passwordHash", rs.getString("password_hash"), "role", rs.getString("role"));
        }, email);

        if (row == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login");
        }

        String dbPass = row.get("passwordHash") == null ? "" : row.get("passwordHash").toString();
        if (!dbPass.equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login");
        }

        LoginResponse out = new LoginResponse();
        out.setSuccess(true);
        out.setToken("dev-token");
        out.setRole(row.get("role") == null ? null : row.get("role").toString());
        out.setUser(Map.of("email", email));
        return out;
    }
}