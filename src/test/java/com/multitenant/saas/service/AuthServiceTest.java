package com.multitenant.saas.service;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.AuthRequest;
import com.multitenant.saas.dto.AuthResponse;
import com.multitenant.saas.exception.DuplicateResourceException;
import com.multitenant.saas.exception.UnauthorizedException;
import com.multitenant.saas.model.User;
import com.multitenant.saas.repository.UserRepository;
import com.multitenant.saas.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-1");

        sampleUser = User.builder()
                .email("test@example.com")
                .password("encoded-password")
                .roles(Set.of("USER"))
                .active(true)
                .build();
        sampleUser.setTenantId("tenant-1");
        sampleUser.setId("user-1");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() {
        when(userRepository.findByTenantIdAndEmail("tenant-1", "test@example.com"))
                .thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(eq("test@example.com"), eq("tenant-1"), anySet()))
                .thenReturn("jwt-token");

        AuthRequest request = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("tenant-1", response.getTenantId());
    }

    @Test
    @DisplayName("Should throw on invalid email")
    void shouldThrowOnInvalidEmail() {
        when(userRepository.findByTenantIdAndEmail("tenant-1", "wrong@example.com"))
                .thenReturn(Optional.empty());

        AuthRequest request = AuthRequest.builder()
                .email("wrong@example.com")
                .password("password123")
                .build();

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should throw on wrong password")
    void shouldThrowOnWrongPassword() {
        when(userRepository.findByTenantIdAndEmail("tenant-1", "test@example.com"))
                .thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        AuthRequest request = AuthRequest.builder()
                .email("test@example.com")
                .password("wrong-password")
                .build();

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should throw when account is disabled")
    void shouldThrowWhenAccountDisabled() {
        sampleUser.setActive(false);
        when(userRepository.findByTenantIdAndEmail("tenant-1", "test@example.com"))
                .thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        AuthRequest request = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void shouldRegisterSuccessfully() {
        when(userRepository.existsByTenantIdAndEmail("tenant-1", "new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtUtil.generateToken(anyString(), eq("tenant-1"), anySet())).thenReturn("jwt-token");

        AuthRequest request = AuthRequest.builder()
                .email("new@example.com")
                .password("password123")
                .build();

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw on duplicate registration")
    void shouldThrowOnDuplicateRegistration() {
        when(userRepository.existsByTenantIdAndEmail("tenant-1", "test@example.com")).thenReturn(true);

        AuthRequest request = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }
}

