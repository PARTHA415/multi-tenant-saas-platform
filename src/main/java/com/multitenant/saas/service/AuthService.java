package com.multitenant.saas.service;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.AuthRequest;
import com.multitenant.saas.dto.AuthResponse;
import com.multitenant.saas.exception.DuplicateResourceException;
import com.multitenant.saas.exception.UnauthorizedException;
import com.multitenant.saas.model.User;
import com.multitenant.saas.repository.UserRepository;
import com.multitenant.saas.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse login(AuthRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Login attempt for email: {} in tenant: {}", request.getEmail(), tenantId);

        User user = userRepository.findByTenantIdAndEmail(tenantId, request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is disabled");
        }

        String token = jwtUtil.generateToken(user.getEmail(), tenantId, user.getRoles());
        log.info("Login successful for email: {} in tenant: {}", request.getEmail(), tenantId);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .tenantId(tenantId)
                .roles(user.getRoles())
                .build();
    }

    public AuthResponse register(AuthRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("Registering user: {} for tenant: {}", request.getEmail(), tenantId);

        if (userRepository.existsByTenantIdAndEmail(tenantId, request.getEmail())) {
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of("USER"))
                .active(true)
                .build();
        user.setTenantId(tenantId);

        userRepository.save(user);
        log.info("User registered successfully: {} for tenant: {}", request.getEmail(), tenantId);

        String token = jwtUtil.generateToken(user.getEmail(), tenantId, user.getRoles());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .tenantId(tenantId)
                .roles(user.getRoles())
                .build();
    }
}


