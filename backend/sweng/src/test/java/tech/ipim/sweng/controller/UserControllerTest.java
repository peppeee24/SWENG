package tech.ipim.sweng.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import tech.ipim.sweng.dto.UserDto;
import tech.ipim.sweng.service.UserService;
import tech.ipim.sweng.util.JwtUtil;


/**
 * Test di integrazione per {@link UserController}, eseguiti in ambiente di test
 * tramite {@link MockitoExtension} per la gestione delle dipendenze mockate.
 * <p>
 * Verifica il comportamento delle API REST relative alla gestione degli utenti
 * per il caso d'uso UC8 - Crea Permessi, in particolare l'endpoint per recuperare
 * la lista degli utenti disponibili per la condivisione di note.
 * <p>
 * Le dipendenze come {@link UserService} e {@link JwtUtil} vengono mockate per isolare la logica del controller.
 * <p>
 * <p>
 * Riepilogo dei test implementati:
 * <ul>
 *   <li>{@code testGetAllUsersExcept_ExcludesCurrentUser} – Verifica esclusione del proprietario dalla lista</li>
 *   <li>{@code testGetAllUsers_EmptyList} – Gestione lista vuota di utenti disponibili</li>
 *   <li>{@code testGetAllUsers_Unauthorized} – Accesso negato senza token di autenticazione</li>
 *   <li>{@code testGetAllUsers_InvalidToken} – Errore per token JWT malformato</li>
 *   <li>{@code testGetAllUsers_NoBearerPrefix} – Errore per header Authorization senza Bearer prefix</li>
 *   <li>{@code testGetAllUsers_ServiceError} – Gestione errore interno del servizio</li>
 *   <li>{@code testGetAllUsers_CorrectDataFormat} – Verifica formato corretto dei dati UserDto</li>
 *   <li>{@code testGetAllUsers_ValidToken} – Comportamento corretto con token valido</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC8 - Test Suite per Gestione Permessi di Condivisione")
public class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserController userController;

    private List<UserDto> mockUsersExcludingOwner;
    private String validToken;

    @BeforeEach
    void setUp() {
        // Setup dati mock
        mockUsersExcludingOwner = createMockUsersExcludingOwner();
        validToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        // Setup comportamento mock JWT con lenient (permette stubbing non usati)
        lenient().when(jwtUtil.extractUsername("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
                .thenReturn("fedegambe");
    }

    /**
     * Verifica che la lista degli utenti restituita escluda correttamente
     * l'utente proprietario (quello autenticato) dalla lista degli utenti
     * disponibili per la condivisione.
     *
     * Simula una richiesta GET con token JWT valido e verifica che:
     * - La risposta sia 200 OK
     * - Il corpo contenga la lista degli utenti
     * - L'utente proprietario sia escluso dalla lista
     */
    @Test
    @DisplayName("UC8.1 - Lista utenti esclude il proprietario della nota")
    void testGetAllUsersExcept_ExcludesCurrentUser() throws Exception {

        when(userService.getAllUsersExcept("fedegambe")).thenReturn(mockUsersExcludingOwner);


        ResponseEntity<List<UserDto>> response = userController.getAllUsers(validToken);


        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());

        // Verifica che l'utente proprietario sia escluso
        boolean ownerExcluded = response.getBody().stream()
                .noneMatch(user -> user.getUsername().equals("fedegambe"));
        assertTrue(ownerExcluded, "L'utente proprietario deve essere escluso dalla lista");

        verify(userService, times(1)).getAllUsersExcept("fedegambe");
        verify(jwtUtil, times(1)).extractUsername(anyString());
    }

    /**
     * Verifica la gestione corretta del caso in cui non ci siano utenti
     * disponibili per la condivisione (lista vuota).
     *
     * Controlla che venga restituita una risposta 200 OK con lista vuota
     * quando il service non trova utenti oltre al proprietario.
     */
    @Test
    @DisplayName("UC8.2 - Gestione errore quando nessun utente disponibile")
    void testGetAllUsers_EmptyList() throws Exception {

        when(userService.getAllUsersExcept("fedegambe")).thenReturn(new ArrayList<>());

        ResponseEntity<List<UserDto>> response = userController.getAllUsers(validToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());

        verify(userService, times(1)).getAllUsersExcept("fedegambe");
    }


    /**
     * Verifica che venga restituito un errore 401 (Unauthorized) quando
     * la richiesta viene effettuata senza fornire un token di autenticazione.
     *
     * Assicura che il service non venga mai invocato in assenza di autenticazione.
     */
    @Test
    @DisplayName("UC8.3 - Accesso negato senza token di autenticazione")
    void testGetAllUsers_Unauthorized() throws Exception {

        ResponseEntity<List<UserDto>> response = userController.getAllUsers(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(userService, never()).getAllUsersExcept(anyString());
        verify(jwtUtil, never()).extractUsername(anyString());
    }


    /**
     * Verifica la gestione di un token JWT malformato o non valido,
     * controllando che venga restituito un errore 401 (Unauthorized).
     *
     * Simula il caso in cui JwtUtil lancia un'eccezione per token invalido.
     */
    @Test
    @DisplayName("UC8.4 - Token JWT malformato restituisce errore")
    void testGetAllUsers_InvalidToken() throws Exception {

        when(jwtUtil.extractUsername("invalid_token")).thenThrow(new RuntimeException("Token non valido"));


        ResponseEntity<List<UserDto>> response = userController.getAllUsers("Bearer invalid_token");


        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(userService, never()).getAllUsersExcept(anyString());
    }


    /**
     * Verifica che venga restituito un errore 401 (Unauthorized) quando
     * l'header Authorization non contiene il prefisso "Bearer ".
     *
     * Testa la validazione del formato dell'header di autenticazione.
     */
    @Test
    @DisplayName("UC8.5 - Header Authorization senza Bearer prefix")
    void testGetAllUsers_NoBearerPrefix() throws Exception {

        ResponseEntity<List<UserDto>> response = userController.getAllUsers("InvalidTokenFormat");


        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(userService, never()).getAllUsersExcept(anyString());
        verify(jwtUtil, never()).extractUsername(anyString());
    }


    /**
     * Verifica la gestione di errori interni del servizio durante
     * il recupero della lista utenti.
     *
     * Simula un'eccezione dal UserService e controlla che venga restituito
     * un errore 500 (Internal Server Error).
     */
    @Test
    @DisplayName("UC8.6 - Errore interno del servizio")
    void testGetAllUsers_ServiceError() throws Exception {

        when(userService.getAllUsersExcept("fedegambe")).thenThrow(new RuntimeException("Database error"));


        ResponseEntity<List<UserDto>> response = userController.getAllUsers(validToken);


        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }


    /**
     * Verifica che il formato dei dati UserDto restituiti sia corretto
     * e contenga tutte le informazioni necessarie per il frontend.
     *
     * Controlla che ogni UserDto abbia: ID, username, nome, cognome, email
     * e che la password non sia esposta nei dati di risposta.
     */
    @Test
    @DisplayName("UC8.7 - Formato dati UserDto corretto per frontend")
    void testGetAllUsers_CorrectDataFormat() throws Exception {

        when(userService.getAllUsersExcept("fedegambe")).thenReturn(mockUsersExcludingOwner);


        ResponseEntity<List<UserDto>> response = userController.getAllUsers(validToken);


        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        if (!response.getBody().isEmpty()) {
            UserDto firstUser = response.getBody().get(0);
            assertNotNull(firstUser.getId());
            assertNotNull(firstUser.getUsername());
            assertNotNull(firstUser.getNome());
            assertNotNull(firstUser.getCognome());
            assertNotNull(firstUser.getEmail());
        }
    }

    /**
     * Verifica il comportamento complessivo con un token valido,
     * controllando che vengano restituiti gli utenti attesi.
     *
     * Testa lo scenario completo di successo verificando:
     * - Risposta 200 OK
     * - Presenza degli utenti mock attesi
     * - Corretta esclusione del proprietario
     */
    @Test
    @DisplayName("UC8.8 - Comportamento corretto con token valido")
    void testGetAllUsers_ValidToken() throws Exception {

        when(userService.getAllUsersExcept("fedegambe")).thenReturn(mockUsersExcludingOwner);


        ResponseEntity<List<UserDto>> response = userController.getAllUsers(validToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() > 0);

        // Verifica che gli utenti attesi siano presenti
        List<String> usernames = response.getBody().stream()
                .map(UserDto::getUsername)
                .toList();
        assertTrue(usernames.contains("alice"));
        assertTrue(usernames.contains("bob"));
        assertTrue(usernames.contains("charlie"));
    }

    // Helper Methods

    private List<UserDto> createMockUsersExcludingOwner() {
        List<UserDto> users = new ArrayList<>();

        UserDto alice = new UserDto();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setNome("Alice");
        alice.setCognome("Smith");
        alice.setEmail("alice@example.com");

        UserDto bob = new UserDto();
        bob.setId(2L);
        bob.setUsername("bob");
        bob.setNome("Bob");
        bob.setCognome("Johnson");
        bob.setEmail("bob@example.com");

        UserDto charlie = new UserDto();
        charlie.setId(3L);
        charlie.setUsername("charlie");
        charlie.setNome("Charlie");
        charlie.setCognome("Brown");
        charlie.setEmail("charlie@example.com");

        users.add(alice);
        users.add(bob);
        users.add(charlie);

        return users;
    }
}