package com.replica.bellavistaerp.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request and response shapes for the auth endpoints. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Email or staff number is required") String identifier,
            @NotBlank(message = "Password is required") String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(max = 30) String staffNumber,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 100) String department,
            UserRole role,
            @NotBlank
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,100}$",
                    message = "Password needs 8+ characters with upper, lower and a number")
            String password) {
    }

    public record UserResponse(
            Long id,
            String fullName,
            String staffNumber,
            String email,
            String department,
            UserRole role,
            boolean active) {

        public static UserResponse of(AppUser u) {
            return new UserResponse(u.getId(), u.getFullName(), u.getStaffNumber(),
                    u.getEmail(), u.getDepartment(), u.getRole(), u.isActive());
        }
    }

    public record AuthResponse(String token, UserResponse user) {
    }
}
