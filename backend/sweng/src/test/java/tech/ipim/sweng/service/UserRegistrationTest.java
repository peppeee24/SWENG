package tech.ipim.sweng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import tech.ipim.sweng.dto.RegistrationRequest;
import tech.ipim.sweng.dto.RegistrationResponse;
import tech.ipim.sweng.exception.UserAlreadyExistsException;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.util.JwtUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitari per la registrazione utenti tramite il servizio {@link UserService}.
 * <p>
 * Questi test verificano i vari scenari di registrazione, inclusi i casi di
 * successo, tentativi di registrazione con username o email già esistenti,
 * e la registrazione senza email opzionale.
 * <p>
 * Le dipendenze {@link UserRepository}, {@link PasswordEncoder} e {@link JwtUtil}
 * sono mockate per isolare la logica di business del servizio.
 */

@ExtendWith(MockitoExtension.class)
public class UserRegistrationTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @InjectMocks
    private UserService userService;

    private RegistrationRequest validRequest;

    /**
     * Setup iniziale comune a tutti i test.
     * <p>
     * Crea una richiesta di registrazione valida con username, password ed email.
     */

    @BeforeEach
    void setUp() {
        validRequest = new RegistrationRequest();
        validRequest.setUsername("testuser");
        validRequest.setPassword("password123");
        validRequest.setEmail("test@example.com");
    }

     /**
     * Test di registrazione con dati validi.
     * <p>
     * Mocka il repository per indicare che username ed email sono liberi,
     * mocka l'encoding della password e il salvataggio utente.
     * Verifica che la risposta indichi successo e che i dati ritornati
     * corrispondano a quelli della richiesta.
     * Inoltre, verifica che siano state chiamate tutte le interazioni previste.
     */
    @Test
    void shouldRegisterNewUserWithValidData() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@example.com");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegistrationResponse response = userService.registerUser(validRequest);

        assertTrue(response.isSuccess());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    /**
     * Test di registrazione fallita a causa di username già esistente.
     * <p>
     * Mocka il repository per indicare che lo username esiste già.
     * Verifica che venga lanciata l'eccezione {@link UserAlreadyExistsException}
     * con messaggio corretto e che non venga effettuato il salvataggio utente.
     */

    @Test
    void shouldFailWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
            UserAlreadyExistsException.class,
            () -> userService.registerUser(validRequest)
        );

        assertEquals("Username 'testuser' è già in uso", exception.getMessage());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

     /**
     * Test di registrazione fallita a causa di email già esistente.
     * <p>
     * Mocka il repository per indicare che lo username è libero ma l'email è già in uso.
     * Verifica che venga lanciata l'eccezione {@link UserAlreadyExistsException}
     * con messaggio corretto e che non venga effettuato il salvataggio utente.
     */

    @Test
    void shouldFailWhenEmailAlreadyExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
            UserAlreadyExistsException.class,
            () -> userService.registerUser(validRequest)
        );

        assertEquals("Email 'test@example.com' è già in uso", exception.getMessage());
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * Test di registrazione con email opzionale NON fornita.
     * <p>
     * Mocka il repository per indicare che lo username è libero,
     * mocka l'encoding della password e il salvataggio utente.
     * Verifica che la registrazione abbia successo, che l'email nel
     * risultato sia nulla e che non venga mai verificata la presenza email.
     */
    
    @Test
    void shouldRegisterUserWithoutOptionalEmail() {
        RegistrationRequest requestNoEmail = new RegistrationRequest();
        requestNoEmail.setUsername("testuser");
        requestNoEmail.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegistrationResponse response = userService.registerUser(requestNoEmail);

        assertTrue(response.isSuccess());
        assertEquals("testuser", response.getUsername());
        assertNull(response.getEmail());
        
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository).save(any(User.class));
    }
}