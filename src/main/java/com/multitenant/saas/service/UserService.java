package com.multitenant.saas.service;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.UserDTO;
import com.multitenant.saas.exception.DuplicateResourceException;
import com.multitenant.saas.exception.ResourceNotFoundException;
import com.multitenant.saas.model.User;
import com.multitenant.saas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO createUser(UserDTO dto) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating user: {} for tenant: {}", dto.getEmail(), tenantId);

        if (userRepository.existsByTenantIdAndEmail(tenantId, dto.getEmail())) {
            throw new DuplicateResourceException("User with email '" + dto.getEmail() + "' already exists in this tenant");
        }

        Set<String> roles = dto.getRoles() != null ? dto.getRoles() : new HashSet<>(Set.of("USER"));

        User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .roles(roles)
                .active(true)
                .build();
        user.setTenantId(tenantId);

        user = userRepository.save(user);
        log.info("User created with ID: {} for tenant: {}", user.getId(), tenantId);

        return toDTO(user);
    }

    public List<UserDTO> getAllUsers() {
        String tenantId = TenantContext.getTenantId();
        log.debug("Fetching all users for tenant: {}", tenantId);
        return userRepository.findAllByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(String id) {
        String tenantId = TenantContext.getTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return toDTO(user);
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles())
                .active(user.isActive())
                .tenantId(user.getTenantId())
                .build();
    }
}


