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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test di unità per il servizio di autenticazione {@link UserService}.
 * <p>
 * Questi test verificano il flusso di login dell'utente, coprendo casi di successo,
 * fallimento per username non esistente, password errata e password vuota.
 * <p>
 * Vengono usati mock per isolare le dipendenze esterne quali repository utenti,
 * encoder delle password e generatore di token JWT.
 */

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

    /**
     * Setup dati comuni per i test.
     * <p>
     * Inizializza una richiesta di login valida e un utente esistente con password hashed.
     */

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

    /**
     * Test del login con credenziali corrette.
     * <p>
     * Mocka la ricerca dell'utente, la verifica della password e la generazione del token JWT.
     * Verifica che la risposta indichi successo, il messaggio sia corretto,
     * il token JWT sia presente e l'utente restituito abbia username corretto.
     * Inoltre verifica che i metodi mockati siano effettivamente invocati.
     */

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

    /**
     * Test del login con username non esistente.
     * <p>
     * Mocka il repository per restituire Optional vuoto.
     * Verifica che venga lanciata una RuntimeException con messaggio
     * "Credenziali non valide" e che encoder e jwtUtil non vengano chiamati.
     */

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

    /**
     * Test del login con password errata.
     * <p>
     * Mocka la ricerca dell'utente e la verifica password per restituire false.
     * Verifica che venga lanciata una RuntimeException con messaggio
     * "Credenziali non valide" e che il token JWT non venga generato.
     */

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

    /**
     * Test del login con password vuota.
     * <p>
     * Mocka la ricerca dell'utente e verifica password fallita per stringa vuota.
     * Verifica che venga lanciata una RuntimeException con messaggio
     * "Credenziali non valide".
     */
    
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