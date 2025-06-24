package tech.ipim.sweng.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Test
    @DisplayName("UC8.1 - Lista utenti esclude il proprietario della nota")
    void testGetAllUsersExcept_ExcludesCurrentUser() throws Exception {
        // Arrange
        when(userService.getAllUsersExcept("fedegambe")).thenReturn(mockUsersExcludingOwner);

        // Act
        ResponseEntity<List<UserDto>> response = userController.getAllUsers(validToken);

        // Assert
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

    @Test
    @DisplayName("UC8.2 - Gestione errore quando nessun utente disponibile")
    void testGetAllUsers_EmptyList() throws Exception {
        // Arrange
        when(userService.getAllUsersExcept("fedegambe")).thenReturn(new ArrayList<>());

        // Act
        ResponseEntity<List<UserDto>> response = userController.getAllUsers(validToken);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());

        verify(userService, times(1)).getAllUsersExcept("fedegambe");
    }

    @Test
    @DisplayName("UC8.3 - Accesso negato senza token di autenticazione")
    void testGetAllUsers_Unauthorized() throws Exception {
        // Act
        ResponseEntity<List<UserDto>> response = userController.getAllUsers(null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(userService, never()).getAllUsersExcept(anyString());
        verify(jwtUtil, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("UC8.4 - Token JWT malformato restituisce errore")
    void testGetAllUsers_InvalidToken() throws Exception {
        // Arrange
        when(jwtUtil.extractUsername("invalid_token")).thenThrow(new RuntimeException("Token non valido"));

        // Act
        ResponseEntity<List<UserDto>> response = userController.getAllUsers("Bearer invalid_token");

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(userService, never()).getAllUsersExcept(anyString());
    }

    @Test
    @DisplayName("UC8.5 - Header Authorization senza Bearer prefix")
    void testGetAllUsers_NoBearerPrefix() throws Exception {
        // Act
        ResponseEntity<List<UserDto>> response = userController.getAllUsers("InvalidTokenFormat");

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(userService, never()).getAllUsersExcept(anyString());
        verify(jwtUtil, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("UC8.6 - Errore interno del servizio")
    void testGetAllUsers_ServiceError() throws Exception {
        // Arrange
        when(userService.getAllUsersExcept("fedegambe")).thenThrow(new RuntimeException("Database error"));

        // Act
        ResponseEntity<List<UserDto>> response = userController.getAllUsers(validToken);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    @DisplayName("UC8.7 - Formato dati UserDto corretto per frontend")
    void testGetAllUsers_CorrectDataFormat() throws Exception {
        // Arrange
        when(userService.getAllUsersExcept("fedegambe")).thenReturn(mockUsersExcludingOwner);

        // Act
        ResponseEntity<List<UserDto>> response = userController.getAllUsers(validToken);

        // Assert
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

    @Test
    @DisplayName("UC8.8 - Comportamento corretto con token valido")
    void testGetAllUsers_ValidToken() throws Exception {
        // Arrange
        when(userService.getAllUsersExcept("fedegambe")).thenReturn(mockUsersExcludingOwner);

        // Act
        ResponseEntity<List<UserDto>> response = userController.getAllUsers(validToken);

        // Assert
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