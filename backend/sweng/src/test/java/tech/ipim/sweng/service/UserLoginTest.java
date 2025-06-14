package tech.ipim.sweng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import tech.ipim.sweng.dto.LoginRequest;
import tech.ipim.sweng.dto.LoginResponse;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.util.JwtUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserLoginTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @InjectMocks
    private UserService userService;

    private LoginRequest validLoginRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("password123");

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setPassword("hashedPassword");
        existingUser.setEmail("test@example.com");
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(existingUser)).thenReturn("jwt.token.here");


        LoginResponse response = userService.authenticateUser(validLoginRequest);


        assertTrue(response.isSuccess());
        assertEquals("Login effettuato con successo", response.getMessage());
        assertEquals("jwt.token.here", response.getToken());
        assertNotNull(response.getUser());
        assertEquals("testuser", response.getUser().getUsername());
        
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password123", "hashedPassword");
        verify(jwtUtil).generateToken(existingUser);
    }

    @Test
    void shouldFailLoginWithInvalidUsername() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername("nonexistent");
        invalidRequest.setPassword("password123");


        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.authenticateUser(invalidRequest)
        );

        assertEquals("Credenziali non valide", exception.getMessage());
        verify(userRepository).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void shouldFailLoginWithInvalidPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setPassword("wrongpassword");

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.authenticateUser(invalidRequest)
        );

        assertEquals("Credenziali non valide", exception.getMessage());
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("wrongpassword", "hashedPassword");
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void shouldFailLoginWithEmptyPassword() {
        LoginRequest emptyPasswordRequest = new LoginRequest();
        emptyPasswordRequest.setUsername("testuser");
        emptyPasswordRequest.setPassword("");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("", "hashedPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.authenticateUser(emptyPasswordRequest)
        );

        assertEquals("Credenziali non valide", exception.getMessage());
    }
}