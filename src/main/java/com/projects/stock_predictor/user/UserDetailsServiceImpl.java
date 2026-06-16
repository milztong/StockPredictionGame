package com.projects.stock_predictor.user;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Standard Spring Security lookup — used for compatibility.
     * Username is the PulseStack username (JWT subject).
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return toUserDetails(user);
    }

    /**
     * SSO entry point — called by JwtAuthFilter on every authenticated request.
     * If the user doesn't exist locally yet, it is created automatically
     * from the PulseStack JWT (subject = username). No password is stored.
     */
    @Transactional
    public UserDetails loadOrCreateByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User ssoUser = new User(username);
                    return userRepository.save(ssoUser);
                });
        return toUserDetails(user);
    }

    private UserDetails toUserDetails(User user) {
        // SSO users have no password hash — use a placeholder that never matches.
        String password = user.getPasswordHash() != null ? user.getPasswordHash() : "{noop}__sso__";
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                password,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
