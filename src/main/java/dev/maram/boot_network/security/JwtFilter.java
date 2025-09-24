package dev.maram.boot_network.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter { //to make it a filter

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("JWT Filter processing request: {} {}", request.getMethod(), request.getRequestURI());

        if (request.getServletPath().contains("/api/v1/auth")) {
            log.info("Skipping JWT filter for authentication endpoint: {}", request.getServletPath());
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("No valid Authorization header found, skipping JWT processing");
            filterChain.doFilter(request, response);
            return;
        }

        log.info("Authorization header found, extracting JWT token");
        jwt = authHeader.substring(7).trim();
        userEmail = jwtService.extractUserName(jwt);
        log.info("Extracted user email from JWT: {}", userEmail);

        if(userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) { //the second part checks if the user is not already authenticated
            log.info("User not authenticated, loading user details from database");
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail); //checks for the userName in the database

            if(jwtService.isTokenValid(jwt, userDetails)) {
                log.info("JWT token is valid for user: {}", userEmail);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("Authentication set in SecurityContext for user: {}", userEmail);
            } else {
                log.error("JWT token is invalid for user: {}", userEmail);
            }
        } else {
            log.error("User already authenticated or no user email extracted");
        }

        log.info("Continuing filter chain for request: {}", request.getRequestURI());
        filterChain.doFilter(request, response);
    }
}