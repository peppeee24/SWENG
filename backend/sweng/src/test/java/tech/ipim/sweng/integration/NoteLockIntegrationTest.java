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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test di integrazione end-to-end per il sistema di lock delle note,
 * verificando il funzionamento completo della gestione dei blocchi per la modifica collaborativa.
 * <p>
 * Verifica il comportamento delle API REST per il sistema di lock in ambiente
 * {@link SpringBootTest} con database reale, autenticazione JWT e gestione multi-utente.
 * Include test per acquisizione, rilascio, rinnovo lock e integrazione con operazioni di modifica.
 * <p>
 * I test simulano scenari reali di collaborazione tra utenti, validando
 * il comportamento del sistema di lock per prevenire modifiche concorrenti
 * e garantire l'integrità dei dati durante la modifica collaborativa delle note.
 * <p>
 * <p>
 * Riepilogo dei test implementati:
 * <ul>
 *   <li>{@code testLockNote_Success} – Acquisizione corretta del lock su nota</li>
 *   <li>{@code testLockNote_AlreadyLocked} – Gestione conflitto lock già acquisito</li>
 *   <li>{@code testLockNote_NoPermissions} – Blocco tentativo lock senza permessi</li>
 *   <li>{@code testUnlockNote_Success} – Rilascio corretto del lock</li>
 *   <li>{@code testRefreshLock_Success} – Rinnovo del lock esistente</li>
 *   <li>{@code testRefreshLock_NoLock} – Errore rinnovo senza lock attivo</li>
 *   <li>{@code testGetLockStatus_NotLocked} – Stato lock per nota non bloccata</li>
 *   <li>{@code testGetLockStatus_Locked} – Stato lock per nota bloccata</li>
 *   <li>{@code testUpdateNote_WithLock} – Modifica nota con lock e auto-rilascio</li>
 *   <li>{@code testUpdateNote_BlockedByOther} – Blocco modifica per lock di altro utente</li>
 *   <li>{@code testLockWorkflow_CompleteScenario} – Workflow completo collaborativo</li>
 *   <li>{@code testInvalidToken} – Gestione token non valido</li>
 *   <li>{@code testNoteNotFound} – Gestione nota inesistente</li>
 * </ul>
 */
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


    /**
     * Verifica che un utente con permessi possa acquisire correttamente
     * il lock su una nota condivisa.
     *
     * Testa l'endpoint POST /api/notes/{id}/lock e verifica che
     * il lock venga assegnato all'utente richiedente.
     */
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

    /**
     * Verifica che il sistema gestisca correttamente il conflitto
     * quando un utente tenta di acquisire un lock già posseduto da altri.
     *
     * Testa la prevenzione di lock multipli e la restituzione
     * di errore 409 Conflict con informazioni sul proprietario del lock.
     */
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

    /**
     * Verifica che utenti senza permessi di modifica non possano
     * acquisire il lock su note a cui non hanno accesso.
     *
     * Testa la sicurezza del sistema di lock contro accessi
     * non autorizzati da utenti esterni alla condivisione.
     */
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


    /**
     * Verifica che il proprietario di un lock possa rilasciarlo
     * correttamente tramite l'endpoint di unlock.
     *
     * Testa l'endpoint DELETE /api/notes/{id}/lock e la liberazione
     * del lock per permettere ad altri utenti di modificare la nota.
     */
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


    /**
     * Verifica che il proprietario di un lock possa rinnovarne
     * la durata per mantenere attiva la sessione di modifica.
     *
     * Testa l'endpoint PUT /api/notes/{id}/lock/refresh per
     * estendere il timeout del lock durante modifiche prolungate.
     */
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


    /**
     * Verifica che il tentativo di rinnovo di un lock non posseduto
     * venga respinto con errore appropriato.
     *
     * Testa che solo il proprietario attuale del lock possa
     * rinnovarne la durata, prevenendo interferenze di altri utenti.
     */
    @Test
    void testRefreshLock_NoLock() throws Exception {
        // User1 prova a rinnovare senza avere il blocco
        mockMvc.perform(put("/api/notes/{id}/lock/refresh", sharedNoteId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Non hai il blocco su questa nota"));
    }


    /**
     * Verifica che lo stato di una nota non bloccata venga
     * restituito correttamente con tutte le informazioni necessarie.
     *
     * Testa l'endpoint GET /api/notes/{id}/lock-status per note
     * libere e disponibili per la modifica.
     */
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


    /**
     * Verifica che lo stato di una nota bloccata includa
     * informazioni sul proprietario e permessi di modifica.
     *
     * Testa la differenza di informazioni restituite al proprietario
     * del lock versus altri utenti con accesso alla nota.
     */
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


    /**
     * Verifica che la modifica di una nota con lock attivo
     * funzioni correttamente e rilasci automaticamente il lock.
     *
     * Testa l'integrazione tra sistema di lock e operazioni di update,
     * verificando l'auto-rilascio dopo modifica completata.
     */
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


    /**
     * Verifica che tentativi di modifica di note bloccate da altri
     * vengano respinti con errore di conflitto appropriato.
     *
     * Testa la protezione contro modifiche concorrenti e la
     * restituzione di informazioni sul proprietario del lock.
     */
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


    /**
     * Verifica il workflow completo di collaborazione multi-utente
     * con acquisizione, rinnovo, modifica e rilascio del lock.
     *
     * Simula uno scenario reale di modifica collaborativa tra più utenti,
     * testando tutte le operazioni di lock in sequenza.
     */
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


    /**
     * Verifica che richieste con token JWT non valido vengano
     * respinte con errore di autenticazione appropriato.
     *
     * Testa la sicurezza del sistema di lock contro
     * tentativi di accesso non autenticati.
     */
    @Test
    void testInvalidToken() throws Exception {
        mockMvc.perform(post("/api/notes/{id}/lock", sharedNoteId)
                        .header("Authorization", "Bearer invalidtoken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token non valido"));
    }


    /**
     * Verifica che operazioni di lock su note inesistenti
     * vengano gestite con errore appropriato.
     *
     * Testa la robustezza del sistema contro richieste
     * con ID nota non validi o inesistenti.
     */
    @Test
    void testNoteNotFound() throws Exception {
        mockMvc.perform(post("/api/notes/999999/lock")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Nota non trovata"));
    }


    // METODI HELPER


    /**
     * Registra un nuovo utente nel sistema per i test di collaborazione.
     * Metodo helper per la configurazione degli utenti di test.
     */
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


    /**
     * Autentica un utente e restituisce il token JWT per le richieste.
     * Metodo helper per l'autenticazione nei test multi-utente.
     */
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


    /**
     * Crea una nota condivisa in scrittura tra user1 e user2.
     * Metodo helper per configurare lo scenario di test collaborativo.
     */
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

        return objectMapper.readTree(response).get("note").get("id").asLong();
    }
}