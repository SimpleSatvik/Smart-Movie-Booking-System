package com.moviebooking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String xRoleHeader = request.getHeader("X-User-Role");

        // If no Authorization header or doesn't start with "Bearer ", check X-User-Role or pass forward
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (xRoleHeader != null && !xRoleHeader.isBlank()) {
                applyFallbackRole(request, xRoleHeader);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the raw token string (everything after "Bearer ")
        final String jwt = authHeader.substring(7).trim();

        try {
            if (jwtUtils.isTokenValid(jwt)) {
                String gmail = jwtUtils.extractGmail(jwt);
                String role = jwtUtils.extractRole(jwt);

                // Store verified identity in the request attributes so any Controller can inspect it
                request.setAttribute("currentUserGmail", gmail);
                request.setAttribute("currentUserRole", role);

                log.debug("JWT verified successfully for user: {} with role: {}", gmail, role);
            } else {
                // Seamless developer/demo fallback for mock tokens or role testing
                String targetRole = (jwt.contains("admin") || (xRoleHeader != null && xRoleHeader.contains("ADMIN"))) ? "ROLE_ADMIN" :
                                    (jwt.contains("owner") || (xRoleHeader != null && xRoleHeader.contains("OWNER"))) ? "ROLE_THEATRE_OWNER" :
                                    (xRoleHeader != null && !xRoleHeader.isBlank()) ? xRoleHeader : "ROLE_USER";
                applyFallbackRole(request, targetRole);
            }
        } catch (Exception e) {
            log.warn("Failed to parse JWT token: {}. Attempting fallback.", e.getMessage());
            if (xRoleHeader != null && !xRoleHeader.isBlank()) {
                applyFallbackRole(request, xRoleHeader);
            }
        }

        // Continue forward to the Controller
        filterChain.doFilter(request, response);
    }

    private void applyFallbackRole(HttpServletRequest request, String roleInput) {
        String role = (roleInput != null && roleInput.toUpperCase().contains("ADMIN")) ? "ROLE_ADMIN" :
                      (roleInput != null && roleInput.toUpperCase().contains("OWNER")) ? "ROLE_THEATRE_OWNER" : "ROLE_USER";
        String email = switch (role) {
            case "ROLE_ADMIN" -> "admin@cineplex.com";
            case "ROLE_THEATRE_OWNER" -> "owner@pvr.com";
            default -> "satvik@gmail.com";
        };
        request.setAttribute("currentUserGmail", email);
        request.setAttribute("currentUserRole", role);
        log.info("Development role applied: {} ({})", email, role);
    }
}
