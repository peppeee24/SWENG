package tech.ipim.sweng.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.Set;
import java.util.Arrays;

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
import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.UpdateNoteRequest;
import tech.ipim.sweng.dto.PermissionDto;
import tech.ipim.sweng.model.TipoPermesso;
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
                .andExpect(jsonPath("$[?(@.username == 'owner_user')]").doesNotExist());
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

    @Test
    @DisplayName("UC4+UC8.I11 - Test flusso completo: Condivisione -> Modifica")
    void testCompleteCollaborationFlow() throws Exception {
        // Arrange: Setup utente collaboratore
        RegistrationRequest collaborator = new RegistrationRequest();
        collaborator.setUsername("collaborator");
        collaborator.setPassword("TestPass123!");
        collaborator.setNome("Collaboratore");
        collaborator.setCognome("Test");
        collaborator.setEmail("collaborator@test.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collaborator)))
                .andExpect(status().isCreated());

        String collaboratorToken = authenticateUser("collaborator", "TestPass123!");

        // 1. Utente A (proprietario) crea una nota
        CreateNoteRequest noteRequest = new CreateNoteRequest();
        noteRequest.setTitolo("Nota Collaborativa");
        noteRequest.setContenuto("Contenuto iniziale della nota");
        noteRequest.setTags(Set.of("collaborazione", "test"));
        noteRequest.setCartelle(Set.of("Progetti"));

        // Imposta permessi condivisione in scrittura
        PermissionDto permessi = new PermissionDto();
        permessi.setTipoPermesso(TipoPermesso.CONDIVISA_SCRITTURA);
        permessi.setUtentiScrittura(Arrays.asList("collaborator"));
        noteRequest.setPermessi(permessi);

        String noteResponse = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Estrai ID nota dalla risposta
        Long noteId = extractNoteIdFromResponse(noteResponse);

        // 2. Verifica che il collaboratore possa vedere la nota condivisa
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + collaboratorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titolo", is("Nota Collaborativa")))
                .andExpect(jsonPath("$.contenuto", is("Contenuto iniziale della nota")))
                .andExpect(jsonPath("$.tipoPermesso", is("CONDIVISA_SCRITTURA")))
                .andExpect(jsonPath("$.canEdit", is(true))); // Il collaboratore può modificare

        // 3. Il collaboratore modifica la nota
        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitolo("Nota Collaborativa - Modificata");
        updateRequest.setContenuto("Contenuto modificato dal collaboratore");
        updateRequest.setTags(Set.of("collaborazione", "test", "modificato"));

        mockMvc.perform(put("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + collaboratorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 4. Verifica che la modifica sia stata salvata
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + authToken) // Proprietario verifica
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titolo", is("Nota Collaborativa - Modificata")))
                .andExpect(jsonPath("$.contenuto", is("Contenuto modificato dal collaboratore")))
                .andExpect(jsonPath("$.tags", hasItem("modificato")));

        // 5. Verifica che entrambi gli utenti vedano la nota nella loro lista
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes[?(@.id == " + noteId + ")].titolo",
                        hasItem("Nota Collaborativa - Modificata")));

        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + collaboratorToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes[?(@.id == " + noteId + ")].titolo",
                        hasItem("Nota Collaborativa - Modificata")));
    }

    @Test
    @DisplayName("UC12.I12 - Test rimozione utente da condivisione")
    void testUserSelfRemovalFromSharedNote() throws Exception {
        // Setup: Crea utente per condivisione
        RegistrationRequest sharedUser = new RegistrationRequest();
        sharedUser.setUsername("shareduser");
        sharedUser.setPassword("TestPass123!");
        sharedUser.setNome("Shared");
        sharedUser.setCognome("User");
        sharedUser.setEmail("shared@test.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sharedUser)))
                .andExpect(status().isCreated());

        String sharedUserToken = authenticateUser("shareduser", "TestPass123!");

        // 1. Proprietario crea nota condivisa
        CreateNoteRequest noteRequest = new CreateNoteRequest();
        noteRequest.setTitolo("Nota per Auto-rimozione");
        noteRequest.setContenuto("Contenuto condiviso");

        PermissionDto permessi = new PermissionDto();
        permessi.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA);
        permessi.setUtentiLettura(Arrays.asList("shareduser"));
        noteRequest.setPermessi(permessi);

        String noteResponse = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noteRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long noteId = extractNoteIdFromResponse(noteResponse);

        // 2. Verifica che l'utente condiviso veda la nota
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + sharedUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titolo", is("Nota per Auto-rimozione")));

        // 3. L'utente condiviso si rimuove dalla condivisione
        mockMvc.perform(delete("/api/notes/" + noteId + "/remove-self")
                        .header("Authorization", "Bearer " + sharedUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("rimosso dalla condivisione")));

        // 4. Verifica che l'utente non possa più accedere alla nota
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + sharedUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 5. Verifica che la nota non appaia più nella sua lista
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + sharedUserToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes[?(@.id == " + noteId + ")]").doesNotExist());
    }

    @Test
    @DisplayName("Security.I13 - Test sicurezza accesso note private")
    void testPrivateNoteSecurityAccess() throws Exception {
        // Setup secondo utente
        RegistrationRequest unauthorizedUser = new RegistrationRequest();
        unauthorizedUser.setUsername("unauthorized");
        unauthorizedUser.setPassword("TestPass123!");
        unauthorizedUser.setNome("Unauthorized");
        unauthorizedUser.setCognome("User");
        unauthorizedUser.setEmail("unauthorized@test.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedUser)))
                .andExpect(status().isCreated());

        String unauthorizedToken = authenticateUser("unauthorized", "TestPass123!");

        // Crea nota privata
        CreateNoteRequest privateNote = new CreateNoteRequest();
        privateNote.setTitolo("Nota Privata");
        privateNote.setContenuto("Contenuto privato e riservato");

        PermissionDto permessi = new PermissionDto();
        permessi.setTipoPermesso(TipoPermesso.PRIVATA);
        privateNote.setPermessi(permessi);

        String noteResponse = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(privateNote)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long noteId = extractNoteIdFromResponse(noteResponse);

        // Utente non autorizzato tenta di accedere alla nota privata
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + unauthorizedToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Utente non autorizzato tenta di modificare la nota privata
        UpdateNoteRequest updateAttempt = new UpdateNoteRequest();
        updateAttempt.setTitolo("Tentativo Hack");
        updateAttempt.setContenuto("Contenuto non autorizzato");

        mockMvc.perform(put("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + unauthorizedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAttempt)))
                .andExpect(status().isForbidden());

        // Verifica che il proprietario possa ancora accedere normalmente
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titolo", is("Nota Privata")));
    }

    // Metodi helper per i test
    private String authenticateUser(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

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

    private Long extractNoteIdFromResponse(String response) throws Exception {
        // Parsing semplificato per estrarre l'ID
        // In un'implementazione reale useresti ObjectMapper per parsing completo
        if (response.contains("\"id\":")) {
            String idPart = response.substring(response.indexOf("\"id\":") + 5);
            String idString = idPart.substring(0, idPart.indexOf(","));
            return Long.parseLong(idString.trim());
        }
        return 1L; // Fallback per i test
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