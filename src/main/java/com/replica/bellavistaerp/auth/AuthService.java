package com.replica.bellavistaerp.auth;

import com.replica.bellavistaerp.auth.AuthDtos.AuthResponse;
import com.replica.bellavistaerp.auth.AuthDtos.RegisterRequest;
import com.replica.bellavistaerp.auth.AuthDtos.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sign-in, registration and user administration.
 * Deliberately not @Transactional: failed-attempt counters must be saved even
 * when the login then fails with an exception.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final String BAD_LOGIN = "Incorrect email / staff number or password.";

    private final AppUserRepository users;
    private final PasswordHasher hasher;
    private final TokenService tokens;


    public AuthResponse login(String identifier, String password) {
        String id = identifier.trim();
        AppUser user = users.findByEmailIgnoreCase(id)
                .or(() -> users.findByStaffNumberIgnoreCase(id))
                .orElse(null);

        if (user == null) {
            hasher.matches(password, hasher.hash("not-a-real-password"));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_LOGIN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && now.isBefore(user.getLockedUntil())) {
            long minutes = java.time.Duration.between(now, user.getLockedUntil()).toMinutes() + 1;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Try again in " + minutes + " min.");
        }

        if (!hasher.matches(password, user.getPasswordHash())) {
            int attempts = user.getFailedAttempts() + 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
                user.setFailedAttempts(0);
                users.save(user);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many failed attempts. Locked for " + LOCK_MINUTES + " minutes.");
            }
            user.setFailedAttempts(attempts);
            users.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, BAD_LOGIN);
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your account is pending approval by Head Office Admin.");
        }

        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        users.save(user);
        return new AuthResponse(tokens.issue(user), UserResponse.of(user));
    }

    public UserResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        String staff = r.staffNumber().trim();
        if (users.existsByEmailIgnoreCase(email) || users.existsByStaffNumberIgnoreCase(staff)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with that email or staff number already exists.");
        }
        AppUser u = new AppUser();
        u.setFullName(r.fullName().trim());
        u.setStaffNumber(staff);
        u.setEmail(email);
        u.setDepartment(r.department().trim());
        u.setRole(r.role() == null ? UserRole.REQUESTER : r.role());
        u.setPasswordHash(hasher.hash(r.password()));
        u.setActive(false); // Head Office must approve
        return UserResponse.of(users.save(u));
    }

    public List<UserResponse> findAll() {
        return users.findAll().stream().map(UserResponse::of).toList();
    }

    public UserResponse approve(Long id, UserRole role) {
        AppUser u = get(id);
        if (role != null) {
            u.setRole(role);
        }
        u.setActive(true);
        return UserResponse.of(users.save(u));
    }

    public UserResponse setRole(Long id, UserRole role) {
        AppUser u = get(id);
        u.setRole(role);
        return UserResponse.of(users.save(u));
    }

    public UserResponse deactivate(Long id) {
        AppUser u = get(id);
        u.setActive(false);
        return UserResponse.of(users.save(u));
    }

    private AppUser get(Long id) {
        return users.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }
}
