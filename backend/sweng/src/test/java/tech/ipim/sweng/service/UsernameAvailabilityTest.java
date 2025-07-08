package tech.ipim.sweng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UsernameAvailabilityTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @InjectMocks
    private UserService userService;

    @Test
    void shouldReturnTrueWhenUsernameIsAvailable() {
        String availableUsername = "newuser";
        when(userRepository.existsByUsername(availableUsername)).thenReturn(false);

        boolean isAvailable = userService.isUsernameAvailable(availableUsername);

        assertTrue(isAvailable);
        verify(userRepository).existsByUsername(availableUsername);
    }

    @Test
    void shouldReturnFalseWhenUsernameIsNotAvailable() {
        String takenUsername = "existinguser";
        when(userRepository.existsByUsername(takenUsername)).thenReturn(true);

        boolean isAvailable = userService.isUsernameAvailable(takenUsername);

        assertFalse(isAvailable);
        verify(userRepository).existsByUsername(takenUsername);
    }

    @Test
    void shouldReturnTrueWhenEmailIsAvailable() {
        String availableEmail = "new@example.com";
        when(userRepository.existsByEmail(availableEmail)).thenReturn(false);

        boolean isAvailable = userService.isEmailAvailable(availableEmail);

        assertTrue(isAvailable);
        verify(userRepository).existsByEmail(availableEmail);
    }

    @Test
    void shouldReturnFalseWhenEmailIsNotAvailable() {
        String takenEmail = "existing@example.com";
        when(userRepository.existsByEmail(takenEmail)).thenReturn(true);

        boolean isAvailable = userService.isEmailAvailable(takenEmail);

        assertFalse(isAvailable);
        verify(userRepository).existsByEmail(takenEmail);
    }

    @Test
    void shouldHandleCaseInsensitiveUsernameCheck() {
        String upperCaseUsername = "TESTUSER";
        when(userRepository.existsByUsername(upperCaseUsername)).thenReturn(true);

        boolean isAvailable = userService.isUsernameAvailable(upperCaseUsername);

        assertFalse(isAvailable);
        verify(userRepository).existsByUsername(upperCaseUsername);
    }

    @Test
    void shouldHandleSpecialCharactersInUsername() {
        String specialUsername = "user.name_123";
        when(userRepository.existsByUsername(specialUsername)).thenReturn(false);

        boolean isAvailable = userService.isUsernameAvailable(specialUsername);

        assertTrue(isAvailable);
        verify(userRepository).existsByUsername(specialUsername);
    }
}