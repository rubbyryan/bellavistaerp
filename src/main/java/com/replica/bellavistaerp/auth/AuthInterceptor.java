package com.replica.bellavistaerp.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Requires a valid Bearer token on every /api/** call except login and
 * registration, and applies the role rules below.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ATTRIBUTE = "bv.user";

    private record Rule(String method, Pattern path, Set<UserRole> roles) {
        boolean matches(String m, String p) {
            return (method.equals("*") || method.equalsIgnoreCase(m)) && path.matcher(p).matches();
        }
    }

    /** First matching rule wins. Anything not listed needs only a valid sign-in. */
    private static final java.util.List<Rule> RULES = java.util.List.of(
            new Rule("*", Pattern.compile("^/api/users(/.*)?$"), Set.of(UserRole.HO)),
            new Rule("POST", Pattern.compile("^/api/procurement/requisitions/\\d+/(stores-review|submit)$"),
                    Set.of(UserRole.STORES)),
            new Rule("POST", Pattern.compile("^/api/procurement/requisitions/\\d+/head/(approve|reject)$"),
                    Set.of(UserRole.HEAD)),
            new Rule("POST", Pattern.compile("^/api/procurement/requisitions/\\d+/operations-manager/(approve|reject)$"),
                    Set.of(UserRole.OM)),
            new Rule("POST", Pattern.compile("^/api/procurement/stock-issues/.*$"),
                    Set.of(UserRole.STORES, UserRole.HO)),
            new Rule("POST", Pattern.compile("^/api/procurement/(items|categories|departments|stock-locations)$"),
                    Set.of(UserRole.STORES, UserRole.HO)),
            new Rule("PUT", Pattern.compile("^/api/procurement/(categories|departments|stock-locations)/\\d+$"),
                    Set.of(UserRole.STORES, UserRole.HO)),
            new Rule("PATCH", Pattern.compile("^/api/procurement/(categories|departments|stock-locations)/\\d+/deactivate$"),
                    Set.of(UserRole.STORES, UserRole.HO))
    );

    private final TokenService tokens;
    private final AppUserRepository users;

    @Value("${app.auth.enabled:true}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please sign in.");
            return false;
        }
        Optional<Long> userId = tokens.verify(header.substring(7).trim());
        AppUser user = userId.flatMap(users::findById).orElse(null);
        if (user == null || !user.isActive()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Your session has expired. Please sign in again.");
            return false;
        }

        String method = request.getMethod();
        for (Rule rule : RULES) {
            if (rule.matches(method, path)) {
                if (!rule.roles().contains(user.getRole())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Your role (" + user.getRole() + ") is not allowed to do this.");
                    return false;
                }
                break;
            }
        }
        request.setAttribute(USER_ATTRIBUTE, user);
        return true;
    }
}
