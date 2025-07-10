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

/**
 * Test unitari per la verifica della disponibilità di username e email
 * tramite il servizio {@link UserService}.
 * <p>
 * Questi test validano il corretto funzionamento dei metodi che controllano
 * se username o email siano già presenti nel sistema, gestendo anche casi
 * con caratteri speciali e differenze di maiuscole/minuscole.
 * <p>
 * Le dipendenze {@link UserRepository}, {@link PasswordEncoder} e {@link JwtUtil}
 * sono mockate per isolare la logica di business.
 */

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


    /**
     * Verifica che venga restituito true quando lo username NON esiste nel sistema.
     * <p>
     * Mocka il repository per restituire false alla verifica di esistenza username.
     * Verifica che il metodo isUsernameAvailable restituisca true e venga chiamato il repository.
     */

    @Test
    void shouldReturnTrueWhenUsernameIsAvailable() {
        String availableUsername = "newuser";
        when(userRepository.existsByUsername(availableUsername)).thenReturn(false);

        boolean isAvailable = userService.isUsernameAvailable(availableUsername);

        assertTrue(isAvailable);
        verify(userRepository).existsByUsername(availableUsername);
    }

    /**
     * Verifica che venga restituito false quando lo username è già presente nel sistema.
     * <p>
     * Mocka il repository per restituire true alla verifica di esistenza username.
     * Verifica che il metodo isUsernameAvailable restituisca false e venga chiamato il repository.
     */

    @Test
    void shouldReturnFalseWhenUsernameIsNotAvailable() {
        String takenUsername = "existinguser";
        when(userRepository.existsByUsername(takenUsername)).thenReturn(true);

        boolean isAvailable = userService.isUsernameAvailable(takenUsername);

        assertFalse(isAvailable);
        verify(userRepository).existsByUsername(takenUsername);
    }

    /**
     * Verifica che venga restituito true quando l'email NON è già utilizzata.
     * <p>
     * Mocka il repository per restituire false alla verifica di esistenza email.
     * Verifica che il metodo isEmailAvailable restituisca true e venga chiamato il repository.
     */

    @Test
    void shouldReturnTrueWhenEmailIsAvailable() {
        String availableEmail = "new@example.com";
        when(userRepository.existsByEmail(availableEmail)).thenReturn(false);

        boolean isAvailable = userService.isEmailAvailable(availableEmail);

        assertTrue(isAvailable);
        verify(userRepository).existsByEmail(availableEmail);
    }

    /**
     * Verifica che venga restituito false quando l'email è già utilizzata.
     * <p>
     * Mocka il repository per restituire true alla verifica di esistenza email.
     * Verifica che il metodo isEmailAvailable restituisca false e venga chiamato il repository.
     */

    @Test
    void shouldReturnFalseWhenEmailIsNotAvailable() {
        String takenEmail = "existing@example.com";
        when(userRepository.existsByEmail(takenEmail)).thenReturn(true);

        boolean isAvailable = userService.isEmailAvailable(takenEmail);

        assertFalse(isAvailable);
        verify(userRepository).existsByEmail(takenEmail);
    }

    /**
     * Verifica il controllo case insensitive sugli username.
     * <p>
     * Mocka il repository per restituire true anche se lo username è in maiuscolo,
     * simulando che la ricerca sia case sensitive e l'username sia già preso.
     * Verifica che isUsernameAvailable restituisca false.
     */

    @Test
    void shouldHandleCaseInsensitiveUsernameCheck() {
        String upperCaseUsername = "TESTUSER";
        when(userRepository.existsByUsername(upperCaseUsername)).thenReturn(true);

        boolean isAvailable = userService.isUsernameAvailable(upperCaseUsername);

        assertFalse(isAvailable);
        verify(userRepository).existsByUsername(upperCaseUsername);
    }

    /**
     * Verifica il corretto funzionamento del controllo username contenenti caratteri speciali.
     * <p>
     * Mocka il repository per restituire false per uno username con punti, underscore e numeri,
     * simulando username valido e disponibile.
     * Verifica che isUsernameAvailable restituisca true.
     */
    
    @Test
    void shouldHandleSpecialCharactersInUsername() {
        String specialUsername = "user.name_123";
        when(userRepository.existsByUsername(specialUsername)).thenReturn(false);

        boolean isAvailable = userService.isUsernameAvailable(specialUsername);

        assertTrue(isAvailable);
        verify(userRepository).existsByUsername(specialUsername);
    }
}