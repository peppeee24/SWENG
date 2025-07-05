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

        //  Verifica che ci siano altri utenti disponibili
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

        // L'utente proprietario è escluso, quindi ci sono N-1 utenti
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

        // Verifica che il proprietario non sia nella lista
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

    // ===== AGGIUNGERE QUESTI TEST AL TUO FILE IntegrationTest.java ESISTENTE =====

// ===== NUOVI TEST PER VERSIONAMENTO =====

    @Test
    @DisplayName("INT-VER-001: Workflow completo versionamento - Crea → Modifica → Versiona → Ripristina")
    void testCompleteVersioningWorkflow() throws Exception {
        // Step 1: Crea una nota
        String createNoteRequest = """
            {
                "titolo": "Nota Test Versionamento",
                "contenuto": "Contenuto iniziale per test versioning",
                "tags": ["test", "versioning"]
            }
            """;

        String createResponse = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createNoteRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Estrai ID della nota
        Long noteId = extractNoteIdFromResponse(createResponse);

        // Step 2: Verifica creazione della prima versione
        mockMvc.perform(get("/api/notes/" + noteId + "/versions")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].versionNumber", is(1)));

        // Step 3: Modifica la nota (crea seconda versione)
        String updateNoteRequest = """
            {
                "titolo": "Nota Test Versionamento - Modificata",
                "contenuto": "Contenuto modificato per test versioning",
                "tags": ["test", "versioning", "modificata"]
            }
            """;

        mockMvc.perform(put("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateNoteRequest))
                .andExpect(status().isOk());

        // Step 4: Verifica creazione della seconda versione
        mockMvc.perform(get("/api/notes/" + noteId + "/versions")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].versionNumber", is(2))) // Più recente
                .andExpect(jsonPath("$.data[1].versionNumber", is(1))); // Originale

        // Step 5: Confronta le versioni
        mockMvc.perform(get("/api/notes/" + noteId + "/compare/1/2")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.differences.titleChanged", is(true)))
                .andExpect(jsonPath("$.data.differences.contentChanged", is(true)));

        // Step 6: Ripristina la prima versione
        String restoreRequest = """
            {
                "versionNumber": 1
            }
            """;

        mockMvc.perform(post("/api/notes/" + noteId + "/restore")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restoreRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.titolo", is("Nota Test Versionamento"))); // Titolo originale

        // Step 7: Verifica che sia stata creata una terza versione (ripristino)
        mockMvc.perform(get("/api/notes/" + noteId + "/versions")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    @DisplayName("INT-VER-002: Test sicurezza versionamento - Accesso non autorizzato")
    void testVersioningSecurity_UnauthorizedAccess() throws Exception {
        // Setup: Crea un secondo utente
        String secondUserToken = createAndAuthenticateSecondUser();

        // Step 1: Owner crea una nota
        String createNoteRequest = """
            {
                "titolo": "Nota Privata Owner",
                "contenuto": "Contenuto privato che non deve essere accessibile"
            }
            """;

        String createResponse = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createNoteRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long noteId = extractNoteIdFromResponse(createResponse);

        // Step 2: Secondo utente tenta di accedere alle versioni
        mockMvc.perform(get("/api/notes/" + noteId + "/versions")
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());

        // Step 3: Secondo utente tenta di ripristinare una versione
        mockMvc.perform(post("/api/notes/" + noteId + "/restore")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionNumber\": 1}"))
                .andExpect(status().isForbidden());

        // Step 4: Secondo utente tenta di confrontare versioni
        mockMvc.perform(get("/api/notes/" + noteId + "/compare/1/1")
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());
    }

// ===== NUOVI TEST PER RICERCA AVANZATA =====

    @Test
    @DisplayName("INT-SEARCH-001: Workflow ricerca per autore")
    void testSearchByAuthorWorkflow() throws Exception {
        // Setup: Crea note di diversi autori
        String secondUserToken = createAndAuthenticateSecondUser();

        // Owner crea una nota
        createNoteForTest("Nota Owner 1", "Contenuto owner", authToken);
        createNoteForTest("Nota Owner 2", "Contenuto owner", authToken);

        // Secondo utente crea una nota
        createNoteForTest("Nota User2 1", "Contenuto user2", secondUserToken);

        // Condividi una nota dell'owner con il secondo utente
        Long sharedNoteId = createNoteForTest("Nota Condivisa", "Contenuto condiviso", authToken);
        shareNoteWithUser(sharedNoteId, "target_user_search", "LETTURA");

        // Test 1: Owner cerca le proprie note
        mockMvc.perform(get("/api/notes/search/author")
                        .header("Authorization", "Bearer " + authToken)
                        .param("author", "owner_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(3))); // 3 note dell'owner

        // Test 2: Secondo utente cerca note dell'owner (dovrebbe vedere solo quelle condivise)
        mockMvc.perform(get("/api/notes/search/author")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .param("author", "owner_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1))) // Solo la nota condivisa
                .andExpect(jsonPath("$.data[0].titolo", is("Nota Condivisa")));

        // Test 3: Ricerca autore inesistente
        mockMvc.perform(get("/api/notes/search/author")
                        .header("Authorization", "Bearer " + authToken)
                        .param("author", "utente_inesistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("INT-SEARCH-002: Workflow ricerca per data")
    void testSearchByDateWorkflow() throws Exception {
        // Step 1: Crea note con date diverse (simulando modifica delle date)
        Long noteRecente = createNoteForTest("Nota Recente", "Contenuto recente", authToken);
        Long noteVecchia = createNoteForTest("Nota Vecchia", "Contenuto vecchio", authToken);

        // Step 2: Ricerca nell'ultimo giorno
        String oggi = java.time.LocalDate.now().toString();
        mockMvc.perform(get("/api/notes/search/date")
                        .header("Authorization", "Bearer " + authToken)
                        .param("startDate", oggi + "T00:00:00")
                        .param("endDate", oggi + "T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));

        // Step 3: Ricerca in range futuro (nessun risultato)
        String domani = java.time.LocalDate.now().plusDays(1).toString();
        String dopodomani = java.time.LocalDate.now().plusDays(2).toString();
        mockMvc.perform(get("/api/notes/search/date")
                        .header("Authorization", "Bearer " + authToken)
                        .param("startDate", domani + "T00:00:00")
                        .param("endDate", dopodomani + "T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // Step 4: Test validazione date invalide
        mockMvc.perform(get("/api/notes/search/date")
                        .header("Authorization", "Bearer " + authToken)
                        .param("startDate", "data-invalida")
                        .param("endDate", oggi + "T23:59:59"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("INT-SEARCH-003: Workflow ricerca combinata autore + data")
    void testCombinedSearchWorkflow() throws Exception {
        // Setup
        String secondUserToken = createAndAuthenticateSecondUser();

        // Crea note di diversi autori
        createNoteForTest("Nota Owner Oggi", "Contenuto", authToken);
        createNoteForTest("Nota User2 Oggi", "Contenuto", secondUserToken);

        // Condividi nota del secondo utente con l'owner
        Long sharedNote = createNoteForTest("Nota User2 Condivisa", "Contenuto condiviso", secondUserToken);
        shareNoteFromUserToOwner(sharedNote, secondUserToken);

        // Test ricerca combinata
        String oggi = java.time.LocalDate.now().toString();
        mockMvc.perform(get("/api/notes/search/combined")
                        .header("Authorization", "Bearer " + authToken)
                        .param("author", "target_user_search")
                        .param("startDate", oggi + "T00:00:00")
                        .param("endDate", oggi + "T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

// ===== TEST PERFORMANCE INTEGRATION =====

    @Test
    @DisplayName("INT-PERF-001: Performance ricerca con molte note")
    void testSearchPerformanceWithManyNotes() throws Exception {
        // Step 1: Crea molte note (limitiamo per i test)
        for (int i = 1; i <= 20; i++) {
            createNoteForTest("Nota Performance " + i, "Contenuto " + i, authToken);
        }

        // Step 2: Test performance ricerca per autore
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/notes/search/author")
                        .header("Authorization", "Bearer " + authToken)
                        .param("author", "owner_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(20))));
        long endTime = System.currentTimeMillis();

        // Verifica performance (meno di 200ms)
        assertTrue((endTime - startTime) < 200, "Ricerca per autore troppo lenta: " + (endTime - startTime) + "ms");

        // Step 3: Test performance ricerca per data
        String oggi = java.time.LocalDate.now().toString();
        startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/notes/search/date")
                        .header("Authorization", "Bearer " + authToken)
                        .param("startDate", oggi + "T00:00:00")
                        .param("endDate", oggi + "T23:59:59"))
                .andExpect(status().isOk());
        endTime = System.currentTimeMillis();

        assertTrue((endTime - startTime) < 200, "Ricerca per data troppo lenta: " + (endTime - startTime) + "ms");
    }

// ===== METODI HELPER AGGIUNTIVI =====

    private Long extractNoteIdFromResponse(String responseContent) throws Exception {
        var jsonNode = objectMapper.readTree(responseContent);
        return jsonNode.get("data").get("id").asLong();
    }

    private String createAndAuthenticateSecondUser() throws Exception {
        // Registra secondo utente per i test
        RegistrationRequest secondUser = new RegistrationRequest();
        secondUser.setUsername("target_user_search");
        secondUser.setPassword("SearchPass123!");
        secondUser.setNome("Search");
        secondUser.setCognome("User");
        secondUser.setEmail("search@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUser)))
                .andExpect(status().isCreated());

        // Login del secondo utente
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("target_user_search");
        loginRequest.setPassword("SearchPass123!");

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

    private Long createNoteForTest(String titolo, String contenuto, String token) throws Exception {
        String createRequest = String.format("""
            {
                "titolo": "%s",
                "contenuto": "%s",
                "tags": ["test"]
            }
            """, titolo, contenuto);

        String response = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractNoteIdFromResponse(response);
    }

    private void shareNoteWithUser(Long noteId, String targetUsername, String permissionType) throws Exception {
        String shareRequest = String.format("""
            {
                "tipo": "%s",
                "utentiCondivisi": ["%s"]
            }
            """, permissionType, targetUsername);

        mockMvc.perform(put("/api/notes/" + noteId + "/permissions")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shareRequest))
                .andExpect(status().isOk());
    }

    private void shareNoteFromUserToOwner(Long noteId, String userToken) throws Exception {
        String shareRequest = """
            {
                "tipo": "LETTURA",
                "utentiCondivisi": ["owner_user"]
            }
            """;

        mockMvc.perform(put("/api/notes/" + noteId + "/permissions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shareRequest))
                .andExpect(status().isOk());
    }
}