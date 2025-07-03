package tech.ipim.sweng.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.RegistrationRequest;
import tech.ipim.sweng.dto.LoginRequest;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.dto.PermissionDto;

import java.util.Set;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class NoteLockIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String user1Token;
    private String user2Token;
    private Long sharedNoteId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Registra e ottieni token per user1
        registerUser("user1", "password123", "User", "One", "user1@test.com");
        user1Token = loginAndGetToken("user1", "password123");

        // Registra e ottieni token per user2
        registerUser("user2", "password123", "User", "Two", "user2@test.com");
        user2Token = loginAndGetToken("user2", "password123");

        // Crea una nota condivisa in scrittura
        sharedNoteId = createSharedNote();
    }

    private void registerUser(String username, String password, String nome, String cognome, String email) throws Exception {
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername(username);
        registrationRequest.setPassword(password);
        registrationRequest.setNome(nome);
        registrationRequest.setCognome(cognome);
        registrationRequest.setEmail(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private Long createSharedNote() throws Exception {
        // Crea nota con user1
        CreateNoteRequest createRequest = new CreateNoteRequest();
        createRequest.setTitolo("Nota Condivisa");
        createRequest.setContenuto("Contenuto condiviso");
        createRequest.setTags(Set.of("test"));
        createRequest.setCartelle(Set.of("testfolder"));

        PermissionDto permissions = new PermissionDto();
        permissions.setTipoPermesso(TipoPermesso.CONDIVISA_SCRITTURA);
        // Usa List invece di Set per compatibility con PermissionDto
        permissions.setUtentiLettura(List.of("user2"));
        permissions.setUtentiScrittura(List.of("user2"));
        createRequest.setPermessi(permissions);

        String response = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Usa il path corretto per estrarre l'ID dalla risposta
        return objectMapper.readTree(response).get("note").get("id").asLong();
    }

    @Test
    void testLockNote_Success() throws Exception {
        // User1 (owner) blocca la nota
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nota bloccata con successo"))
                .andExpect(jsonPath("$.lockedBy").value("user1"));
    }

    @Test
    void testLockNote_AlreadyLocked() throws Exception {
        // User1 blocca la nota
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        // User2 prova a bloccare la stessa nota
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Nota già in modifica da altro utente"))
                .andExpect(jsonPath("$.lockedBy").value("user1"));
    }

    @Test
    void testLockNote_NoPermissions() throws Exception {
        // Registra un terzo utente senza permessi
        registerUser("user3", "password123", "User", "Three", "user3@test.com");
        String user3Token = loginAndGetToken("user3", "password123");

        // User3 prova a bloccare la nota senza permessi
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user3Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Non hai i permessi per modificare questa nota"));
    }

    @Test
    void testUnlockNote_Success() throws Exception {
        // User1 blocca la nota
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        // User1 sblocca la nota
        mockMvc.perform(delete("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nota sbloccata con successo"));
    }

    @Test
    void testRefreshLock_Success() throws Exception {
        // User1 blocca la nota
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        // User1 rinnova il blocco
        mockMvc.perform(put("/api/notes/{id}/lock/refresh", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Blocco rinnovato con successo"));
    }

    @Test
    void testRefreshLock_NoLock() throws Exception {
        // User1 prova a rinnovare senza avere il blocco
        mockMvc.perform(put("/api/notes/{id}/lock/refresh", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Non hai il blocco su questa nota"));
    }

    @Test
    void testGetLockStatus_NotLocked() throws Exception {
        // Verifica stato blocco quando non è bloccata
        mockMvc.perform(get("/api/notes/{id}/lock-status", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.canEdit").value(true));
    }

    @Test
    void testGetLockStatus_Locked() throws Exception {
        // User1 blocca la nota
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        // Verifica stato per user1 (ha il blocco)
        mockMvc.perform(get("/api/notes/{id}/lock-status", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.lockedBy").value("user1"))
                .andExpect(jsonPath("$.canEdit").value(true));

        // Verifica stato per user2 (non ha il blocco)
        mockMvc.perform(get("/api/notes/{id}/lock-status", sharedNoteId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.lockedBy").value("user1"))
                .andExpect(jsonPath("$.canEdit").value(false));
    }

    @Test
    void testUpdateNote_WithLock() throws Exception {
        // User1 blocca la nota
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        // User1 aggiorna la nota (dovrebbe sbloccare automaticamente)
        String updateJson = """
            {
                "titolo": "Titolo Aggiornato",
                "contenuto": "Contenuto aggiornato",
                "tags": ["updated"],
                "cartelle": ["updated"]
            }
            """;

        mockMvc.perform(put("/api/notes/{id}", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nota aggiornata con successo"));

        // Verifica che la nota sia stata sbloccata
        mockMvc.perform(get("/api/notes/{id}/lock-status", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false));
    }

    @Test
    void testUpdateNote_BlockedByOther() throws Exception {
        // User1 blocca la nota
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());

        // User2 prova ad aggiornare la nota bloccata
        String updateJson = """
            {
                "titolo": "Tentativo Aggiornamento",
                "contenuto": "Non dovrebbe funzionare",
                "tags": ["fail"],
                "cartelle": ["fail"]
            }
            """;

        mockMvc.perform(put("/api/notes/{id}", sharedNoteId)
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("La nota è in modifica da user1"));
    }

    @Test
    void testLockWorkflow_CompleteScenario() throws Exception {
        // 1. User2 blocca la nota
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockedBy").value("user2"));

        // 2. User1 (owner) non può modificare perché bloccata da user2
        String updateJson = """
            {
                "titolo": "Fallimento",
                "contenuto": "Non dovrebbe funzionare"
            }
            """;

        mockMvc.perform(put("/api/notes/{id}", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isConflict());

        // 3. User2 rinnova il blocco
        mockMvc.perform(put("/api/notes/{id}/lock/refresh", sharedNoteId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk());

        // 4. User2 modifica e sblocca automaticamente
        String successUpdateJson = """
            {
                "titolo": "Modificato da User2",
                "contenuto": "Contenuto modificato con successo"
            }
            """;

        mockMvc.perform(put("/api/notes/{id}", sharedNoteId)
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(successUpdateJson))
                .andExpect(status().isOk());

        // 5. Verifica che la nota sia ora sbloccata
        mockMvc.perform(get("/api/notes/{id}/lock-status", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false));

        // 6. User1 può ora bloccare e modificare
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk());
    }

    @Test
    void testInvalidToken() throws Exception {
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer invalidtoken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token non valido"));
    }

    @Test
    void testNoteNotFound() throws Exception {
        mockMvc.perform(post("/api/notes/999999/lock")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Nota non trovata"));
    }
}