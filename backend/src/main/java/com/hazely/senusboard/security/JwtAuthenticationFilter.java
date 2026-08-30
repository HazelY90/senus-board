package com.hazely.senusboard.security;

import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var authHeader=request.getHeader("Authorization");
        //If authentication is not valid, pass the request to the chain and let the spring deal with it
        if (authHeader==null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request,response);
            return;
        }
        var token=authHeader.replace("Bearer ","");
        var jwt=jwtService.parseToken(token);
        if (jwt==null || jwt.isExpired() || !jwt.isAccess()) {
            filterChain.doFilter(request,response);
            return;
        }
        var user = userRepo.findById(jwt.getId()).orElse(null);
        if (user == null || (user.getStatus() != Status.PENDING && user.getStatus() != Status.ACTIVE)) {
            filterChain.doFilter(request, response);
            return;
        }
        // If the authentication is valid, attach authentication information to the request

        var authentication=new UsernamePasswordAuthenticationToken(
                user.getId(), //id
                null, //password
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))//user role
        );
        // Attach some metadata like IP address to the request.
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        //SecurityContextHolder stores the information of the current authenticated user
        // to allow access to recources

        filterChain.doFilter(request,response);
    }
}
