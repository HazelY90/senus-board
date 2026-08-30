package com.hazely.senusboard.security;

import com.hazely.senusboard.entities.enums.Status;
import com.hazely.senusboard.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String email) {
        var user=userRepository.findByEmail(email).orElseThrow(
                ()->new UsernameNotFoundException("User not found.")
        );
        boolean isEnabled = user.getStatus() == Status.PENDING || user.getStatus() == Status.ACTIVE;
        return User.withUsername(user.getEmail())
                .password(user.getPassword())
                .disabled(!isEnabled)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }

}
