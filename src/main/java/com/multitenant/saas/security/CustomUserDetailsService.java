package com.multitenant.saas.security;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.model.User;
import com.multitenant.saas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String tenantId = TenantContext.getTenantId();
        log.debug("Loading user by email: {} for tenant: {}", email, tenantId);

        User user = userRepository.findByTenantIdAndEmail(tenantId, email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email + " in tenant: " + tenantId));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPassword(), user.isActive(), true, true, true,
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList()));
    }
}
