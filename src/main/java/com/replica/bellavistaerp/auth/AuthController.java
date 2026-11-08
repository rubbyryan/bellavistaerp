package com.replica.bellavistaerp.auth;

import com.replica.bellavistaerp.auth.AuthDtos.AuthResponse;
import com.replica.bellavistaerp.auth.AuthDtos.LoginRequest;
import com.replica.bellavistaerp.auth.AuthDtos.RegisterRequest;
import com.replica.bellavistaerp.auth.AuthDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Sign in, register and check the current user")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Sign in with email or staff number and get a token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.identifier(), request.password());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request an account (stays pending until Head Office approves)")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    @Operation(summary = "The signed-in user")
    public UserResponse me(@RequestAttribute(AuthInterceptor.USER_ATTRIBUTE) AppUser user) {
        return UserResponse.of(user);
    }
}
