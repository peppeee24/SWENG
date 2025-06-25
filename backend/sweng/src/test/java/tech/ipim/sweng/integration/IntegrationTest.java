package tech.ipim.sweng.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.ipim.sweng.dto.LoginRequest;
import tech.ipim.sweng.dto.LoginResponse;
import tech.ipim.sweng.dto.RegistrationRequest;
import tech.ipim.sweng.dto.UserDto;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.service.UserService;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("UC8 - Test di Integrazione End-to-End")
public class IntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Setup: Registra e autentica utenti di test
        setupTestUsers();
        authToken = authenticateTestUser();
    }

    @Test
    @DisplayName("UC8.I1 - Caricamento Utenti escludendo proprietario")
    void testCompleteFlow_RegistrationToUsersList() throws Exception {
        // Act & Assert: Richiesta lista utenti con token valido
        mockMvc.perform(get("/api/users/list")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[?(@.username == 'owner_user')]").doesNotExist()); // AGGIUNTO: Verifica esclusione
    }

    @Test
    @DisplayName("UC8.I2 - Verifica che l'utente proprietario sia escluso dalla lista")
    void testUserListExcludesOwner() throws Exception {
        // Act
        String response = mockMvc.perform(get("/api/users/list")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert
        UserDto[] users = objectMapper.readValue(response, UserDto[].class);

        boolean ownerExcluded = true;
        for (UserDto user : users) {
            if ("owner_user".equals(user.getUsername())) {
                ownerExcluded = false;
                break;
            }
        }

        assertTrue(ownerExcluded, "L'utente proprietario deve essere escluso dalla lista");

        // AGGIUNTO: Verifica che ci siano altri utenti disponibili
        assertTrue(users.length >= 2, "Devono esserci almeno 2 utenti disponibili per la condivisione");
    }

    @Test
    @DisplayName("UC8.I3 - Test con database reale: Persistenza e recupero utenti")
    void testRealDatabase_UserPersistenceAndRetrieval() throws Exception {
        // Arrange: Verifica che gli utenti siano stati salvati nel database
        long userCount = userRepository.count();
        assertTrue(userCount >= 3, "Dovrebbero esserci almeno 3 utenti nel database");

        // Act: Recupera utenti tramite endpoint
        String response = mockMvc.perform(get("/api/users/list")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert: Verifica coerenza con database
        UserDto[] users = objectMapper.readValue(response, UserDto[].class);

        // CORRETTO: L'utente proprietario è escluso, quindi ci sono N-1 utenti
        assertEquals(userCount - 1, users.length, "La lista dovrebbe contenere tutti gli utenti tranne il proprietario");
    }

    @Test
    @DisplayName("UC8.I4 - Test sicurezza: Token scaduto/non valido")
    void testSecurity_InvalidToken() throws Exception {
        // Test con token non valido
        mockMvc.perform(get("/api/users/list")
                        .header("Authorization", "Bearer invalid_token_here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // Test senza token
        mockMvc.perform(get("/api/users/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("UC8.I5 - Test CORS: Verifica configurazione per frontend")
    void testCors_ConfigurationForFrontend() throws Exception {
        mockMvc.perform(get("/api/users/list")
                        .header("Authorization", "Bearer " + authToken)
                        .header("Origin", "http://localhost:4200")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    @DisplayName("UC8.I6 - Test prestazioni: Tempo di risposta accettabile")
    void testPerformance_ResponseTime() throws Exception {
        // Misura tempo di risposta
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/users/list")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Assert: Tempo di risposta < 500ms per buona UX
        assertTrue(responseTime < 500, "Il tempo di risposta dovrebbe essere inferiore a 500ms");
    }

    @Test
    @DisplayName("UC8.I7 - Test concorrenza: Accessi simultanei")
    void testConcurrency_SimultaneousAccess() throws Exception {
        // Simula più richieste contemporanee
        Thread[] threads = new Thread[5];
        boolean[] results = new boolean[5];

        for (int i = 0; i < 5; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    mockMvc.perform(get("/api/users/list")
                                    .header("Authorization", "Bearer " + authToken)
                                    .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isOk());
                    results[index] = true;
                } catch (Exception e) {
                    results[index] = false;
                }
            });
        }

        // Avvia tutti i thread
        for (Thread thread : threads) {
            thread.start();
        }

        // Attendi completamento
        for (Thread thread : threads) {
            thread.join();
        }

        // Verifica che tutte le richieste siano andate a buon fine
        for (boolean result : results) {
            assertTrue(result, "Tutte le richieste concorrenti dovrebbero essere riuscite");
        }
    }

    @Test
    @DisplayName("UC8.I8 - Test formato risposta JSON conforme alle specifiche")
    void testJsonResponse_CorrectFormat() throws Exception {
        String response = mockMvc.perform(get("/api/users/list")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verifica che la risposta sia un array JSON valido
        UserDto[] users = objectMapper.readValue(response, UserDto[].class);
        assertNotNull(users);

        if (users.length > 0) {
            UserDto firstUser = users[0];
            assertNotNull(firstUser.getId());
            assertNotNull(firstUser.getUsername());
            assertNotNull(firstUser.getNome());
            assertNotNull(firstUser.getCognome());
            // Verifica che la password non sia esposta
            // Nel DTO non dovrebbe esserci un metodo getPassword()
        }
    }

    @Test
    @DisplayName("UC8.I9 - Test robustezza: Gestione utenti con dati speciali")
    void testRobustness_SpecialCharacters() throws Exception {
        // Arrange: Crea utente con caratteri speciali
        RegistrationRequest specialUser = new RegistrationRequest();
        specialUser.setUsername("test_àèìòù");
        specialUser.setPassword("TestPass123!");
        specialUser.setNome("Test'Name");
        specialUser.setCognome("Test-Surname");
        specialUser.setEmail("test.special@domain-test.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(specialUser)))
                .andExpect(status().isCreated());

        // Act & Assert: Verifica che l'utente con caratteri speciali sia incluso
        mockMvc.perform(get("/api/users/list")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'test_àèìòù')]").exists());
    }

    @Test
    @DisplayName("UC8.I10 - Test scenario reale: Creazione nota e selezione utenti")
    void testRealScenario_NoteCreationAndUserSelection() throws Exception {
        // Questo test simula il flusso completo UC8:
        // 1. Utente proprietario vuole condividere una nota
        // 2. Sistema carica lista utenti disponibili (ESCLUDENDO il proprietario)
        // 3. Utente seleziona destinatari della condivisione

        // Step 1: Carica utenti disponibili per condivisione
        String response = mockMvc.perform(get("/api/users/list")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserDto[] availableUsers = objectMapper.readValue(response, UserDto[].class);

        // Step 2: Verifica che ci siano utenti disponibili per la condivisione
        assertTrue(availableUsers.length >= 2,
                "Devono esserci almeno 2 utenti disponibili per la condivisione");

        // AGGIUNTO: Verifica che il proprietario non sia nella lista
        boolean ownerFound = false;
        for (UserDto user : availableUsers) {
            if ("owner_user".equals(user.getUsername())) {
                ownerFound = true;
                break;
            }
        }
        assertFalse(ownerFound, "Il proprietario non deve apparire nella lista degli utenti disponibili");

        // Step 3: Simula selezione di utenti da parte del proprietario
        List<String> selectedUsernames = List.of(
                availableUsers[0].getUsername(),
                availableUsers[1].getUsername()
        );

        // Verifica che gli utenti selezionati esistano nel sistema
        for (String username : selectedUsernames) {
            assertTrue(userRepository.existsByUsername(username),
                    "L'utente selezionato deve esistere nel sistema: " + username);
        }
    }

    // Helper Methods

    private void setupTestUsers() throws Exception {
        // Crea utente proprietario
        RegistrationRequest ownerUser = new RegistrationRequest();
        ownerUser.setUsername("owner_user");
        ownerUser.setPassword("OwnerPass123!");
        ownerUser.setNome("Owner");
        ownerUser.setCognome("User");
        ownerUser.setEmail("owner@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerUser)))
                .andExpect(status().isCreated());

        // Crea utenti target per condivisione
        RegistrationRequest user1 = new RegistrationRequest();
        user1.setUsername("target_user1");
        user1.setPassword("Target1Pass123!");
        user1.setNome("Target1");
        user1.setCognome("User1");
        user1.setEmail("target1@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated());

        RegistrationRequest user2 = new RegistrationRequest();
        user2.setUsername("target_user2");
        user2.setPassword("Target2Pass123!");
        user2.setNome("Target2");
        user2.setCognome("User2");
        user2.setEmail("target2@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated());
    }

    private String authenticateTestUser() throws Exception {
        // Login dell'utente proprietario
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("owner_user");
        loginRequest.setPassword("OwnerPass123!");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        return loginResponse.getToken();
    }
}