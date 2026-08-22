package com.dailymate.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.dailymate.user.repository.UserRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService; private final UserRepository users;
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) { this.jwtService = jwtService; this.users = users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { chain.doFilter(request, response); return; }
        try {
            String subject = jwtService.subject(header.substring(7));
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserPrincipal principal = users.findById(subject).map(UserPrincipal::new).orElse(null);
                if (principal != null && principal.isEnabled() && jwtService.isValid(header.substring(7), principal)) {
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ignored) { SecurityContextHolder.clearContext(); }
        chain.doFilter(request, response);
    }
}
