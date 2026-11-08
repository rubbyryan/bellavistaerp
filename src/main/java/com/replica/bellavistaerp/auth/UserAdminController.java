package com.replica.bellavistaerp.auth;

import com.replica.bellavistaerp.auth.AuthDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Head Office Admin only (enforced in AuthInterceptor). */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users & roles", description = "Approve registrations and assign roles (Head Office only)")
public class UserAdminController {

    private final AuthService authService;

    @GetMapping
    @Operation(summary = "List all users")
    public List<UserResponse> findAll() {
        return authService.findAll();
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve a pending account, optionally setting its role")
    public UserResponse approve(@PathVariable Long id, @RequestParam(required = false) UserRole role) {
        return authService.approve(id, role);
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Change a user's role")
    public UserResponse setRole(@PathVariable Long id, @RequestParam UserRole role) {
        return authService.setRole(id, role);
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Block a user from signing in")
    public UserResponse deactivate(@PathVariable Long id) {
        return authService.deactivate(id);
    }
}
