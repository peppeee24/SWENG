package tech.ipim.sweng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tech.ipim.sweng.config.TestConfig;
import tech.ipim.sweng.dto.CartellaDto;
import tech.ipim.sweng.dto.CreateCartellaRequest;
import tech.ipim.sweng.dto.UpdateCartellaRequest;
import tech.ipim.sweng.service.CartellaService;
import tech.ipim.sweng.util.JwtUtil;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


/**
 * Test di integrazione per {@link CartellaController}, eseguiti in ambiente di test
 * tramite {@link WebMvcTest} con configurazione di sicurezza semplificata tramite {@link TestConfig}.
 * <p>
 * Verifica il comportamento delle API REST relative alla gestione delle cartelle,
 * simulando le richieste HTTP e controllando le risposte JSON.
 * <p>
 * Le dipendenze come {@link CartellaService} e {@link JwtUtil} vengono mockate per isolare la logica del controller.
 * <p>
 * <p>
 * Riepilogo dei test implementati:
 * <ul>
 *   <li>{@code shouldCreateCartellaSuccessfully} – Creazione corretta di una cartella</li>
 *   <li>{@code shouldReturnConflictWhenCartellaAlreadyExists} – Errore 409 se il nome è già usato</li>
 *   <li>{@code shouldGetAllCartelleSuccessfully} – Recupero di tutte le cartelle</li>
 *   <li>{@code shouldGetCartellaByIdSuccessfully} – Recupero di una cartella tramite ID</li>
 *   <li>{@code shouldReturnNotFoundWhenCartellaDoesNotExist} – Errore 404 se la cartella non esiste</li>
 *   <li>{@code shouldUpdateCartellaSuccessfully} – Aggiornamento riuscito di una cartella</li>
 *   <li>{@code shouldReturnConflictWhenUpdatingWithExistingName} – Errore 409 per nome duplicato in update</li>
 *   <li>{@code shouldDeleteCartellaSuccessfully} – Eliminazione corretta della cartella</li>
 *   <li>{@code shouldReturnConflictWhenDeletingCartellaWithNotes} – Errore 409 se contiene note</li>
 *   <li>{@code shouldGetCartelleStatsSuccessfully} – Statistiche delle cartelle</li>
 *   <li>{@code shouldReturnUnauthorizedWithoutValidToken} – Errore 401 per token JWT non valido</li>
 *   <li>{@code shouldReturnBadRequestWithInvalidData} – Errore 400 per nome mancante</li>
 *   <li>{@code shouldReturnBadRequestWithTooLongDescription} – Errore 400 per descrizione troppo lunga</li>
 *   <li>{@code shouldHandleServiceException} – Gestione di eccezione generica del service</li>
 *   <li>{@code shouldHandleInternalServerError} – Gestione di IllegalStateException</li>
 *   <li>{@code shouldValidateCartellaNameLength} – Errore 400 per nome troppo lungo</li>
 *   <li>{@code shouldReturnEmptyCartelleList} – Lista vuota di cartelle</li>
 * </ul>
 */
@WebMvcTest(CartellaController.class)
@Import(TestConfig.class)
@ActiveProfiles("test")
class CartellaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartellaService cartellaService;

    @MockBean
    private JwtUtil jwtUtil;

    private CartellaDto testCartellaDto;
    private CreateCartellaRequest createRequest;
    private UpdateCartellaRequest updateRequest;
    private final String validToken = "Bearer valid.jwt.token";
    private final String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        testCartellaDto = new CartellaDto();
        testCartellaDto.setId(1L);
        testCartellaDto.setNome("Test Cartella");
        testCartellaDto.setDescrizione("Descrizione di test");
        testCartellaDto.setProprietario(testUsername);
        testCartellaDto.setDataCreazione(LocalDateTime.now());
        testCartellaDto.setDataModifica(LocalDateTime.now());
        testCartellaDto.setColore("#667eea");
        testCartellaDto.setNumeroNote(5);

        createRequest = new CreateCartellaRequest();
        createRequest.setNome("Nuova Cartella");
        createRequest.setDescrizione("Descrizione nuova cartella");
        createRequest.setColore("#ff6b6b");

        updateRequest = new UpdateCartellaRequest();
        updateRequest.setNome("Cartella Aggiornata");
        updateRequest.setDescrizione("Descrizione aggiornata");
        updateRequest.setColore("#4ecdc4");

        when(jwtUtil.extractTokenFromHeader(validToken)).thenReturn("valid.jwt.token");
        when(jwtUtil.isTokenValid("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid.jwt.token")).thenReturn(testUsername);
    }

    /**
     * Verifica che la creazione di una cartella con dati validi restituisca
     * una risposta 201 (Created) e includa correttamente i dettagli della cartella creata.
     *
     * Simula una richiesta POST con token JWT valido, verifica il corpo JSON e
     * controlla che il service venga invocato una sola volta.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldCreateCartellaSuccessfully() throws Exception {

        when(cartellaService.createCartella(any(CreateCartellaRequest.class), eq(testUsername)))
                .thenReturn(testCartellaDto);

        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cartella creata con successo"))
                .andExpect(jsonPath("$.cartella.id").value(1))
                .andExpect(jsonPath("$.cartella.nome").value("Test Cartella"))
                .andExpect(jsonPath("$.cartella.descrizione").value("Descrizione di test"))
                .andExpect(jsonPath("$.cartella.colore").value("#667eea"))
                .andExpect(jsonPath("$.cartella.numeroNote").value(5));

        verify(cartellaService).createCartella(any(CreateCartellaRequest.class), eq(testUsername));
    }

    /**
     * Verifica che venga restituito un errore 409 (Conflict) se si tenta di creare
     * una cartella con un nome già esistente per lo stesso utente.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnConflictWhenCartellaAlreadyExists() throws Exception {

        when(cartellaService.createCartella(any(CreateCartellaRequest.class), eq(testUsername)))
                .thenThrow(new RuntimeException("Esiste già una cartella con il nome: Nuova Cartella"));


        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Esiste già una cartella con il nome: Nuova Cartella"));
    }

    /**
     * Verifica che la richiesta GET restituisca correttamente l’elenco
     * delle cartelle dell’utente autenticato.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldGetAllCartelleSuccessfully() throws Exception {

        List<CartellaDto> cartelle = Arrays.asList(testCartellaDto);
        when(cartellaService.getUserCartelle(testUsername)).thenReturn(cartelle);

        mockMvc.perform(get("/api/cartelle")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartelle").isArray())
                .andExpect(jsonPath("$.cartelle", hasSize(1)))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.cartelle[0].nome").value("Test Cartella"));

        verify(cartellaService).getUserCartelle(testUsername);
    }

    /**
     * Verifica che una cartella esistente venga restituita correttamente
     * quando richiesta tramite il suo ID.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldGetCartellaByIdSuccessfully() throws Exception {

        when(cartellaService.getCartellaById(1L, testUsername)).thenReturn(Optional.of(testCartellaDto));


        mockMvc.perform(get("/api/cartelle/1")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cartella trovata"))
                .andExpect(jsonPath("$.cartella.id").value(1))
                .andExpect(jsonPath("$.cartella.nome").value("Test Cartella"));

        verify(cartellaService).getCartellaById(1L, testUsername);
    }


    /**
     * Verifica che venga restituito un errore 404 se si cerca di accedere
     * a una cartella inesistente o non appartenente all’utente.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnNotFoundWhenCartellaDoesNotExist() throws Exception {

        when(cartellaService.getCartellaById(999L, testUsername)).thenReturn(Optional.empty());


        mockMvc.perform(get("/api/cartelle/999")
                .header("Authorization", validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cartella non trovata"));

        verify(cartellaService).getCartellaById(999L, testUsername);
    }

    /**
     * Verifica che una cartella esistente venga aggiornata correttamente
     * tramite una richiesta PUT con dati validi.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldUpdateCartellaSuccessfully() throws Exception {

        when(cartellaService.updateCartella(eq(1L), any(UpdateCartellaRequest.class), eq(testUsername)))
                .thenReturn(testCartellaDto);

   
        mockMvc.perform(put("/api/cartelle/1")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cartella aggiornata con successo"))
                .andExpect(jsonPath("$.cartella.nome").value("Test Cartella"));

        verify(cartellaService).updateCartella(eq(1L), any(UpdateCartellaRequest.class), eq(testUsername));
    }

    /**
     * Verifica che venga restituito un errore 409 se si prova ad aggiornare
     * una cartella con un nome già utilizzato.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnConflictWhenUpdatingWithExistingName() throws Exception {

        when(cartellaService.updateCartella(eq(1L), any(UpdateCartellaRequest.class), eq(testUsername)))
                .thenThrow(new RuntimeException("Esiste già una cartella con il nome: Cartella Aggiornata"));


        mockMvc.perform(put("/api/cartelle/1")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Esiste già una cartella con il nome: Cartella Aggiornata"));
    }

    /**
     * Verifica che una cartella venga eliminata correttamente quando non contiene note
     * e appartiene all’utente autenticato.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldDeleteCartellaSuccessfully() throws Exception {

        when(cartellaService.deleteCartella(1L, testUsername)).thenReturn(true);


        mockMvc.perform(delete("/api/cartelle/1")
                .header("Authorization", validToken)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cartella eliminata con successo"));

        verify(cartellaService).deleteCartella(1L, testUsername);
    }


    /**
     * Verifica che venga restituito un errore 409 se si tenta di eliminare
     * una cartella contenente note.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnConflictWhenDeletingCartellaWithNotes() throws Exception {

        when(cartellaService.deleteCartella(1L, testUsername))
                .thenThrow(new RuntimeException("Impossibile eliminare la cartella: contiene 5 note. Sposta prima le note."));

        // When & Then
        mockMvc.perform(delete("/api/cartelle/1")
                .header("Authorization", validToken)
                .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Impossibile eliminare la cartella: contiene 5 note. Sposta prima le note."));
    }


    /**
     * Verifica che le statistiche delle cartelle dell’utente vengano recuperate correttamente
     * (numero totale e nomi delle cartelle).
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldGetCartelleStatsSuccessfully() throws Exception {
        CartellaService.CartelleStats stats = new CartellaService.CartelleStats(
                3L, Arrays.asList("Lavoro", "Personale", "Studio")
        );
        when(cartellaService.getUserCartelleStats(testUsername)).thenReturn(stats);

        mockMvc.perform(get("/api/cartelle/stats")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.numeroCartelle").value(3))
                .andExpect(jsonPath("$.stats.nomiCartelle", hasSize(3)))
                .andExpect(jsonPath("$.stats.nomiCartelle", hasItems("Lavoro", "Personale", "Studio")));

        verify(cartellaService).getUserCartelleStats(testUsername);
    }

    /**
     * Verifica che venga restituito un errore 401 (Unauthorized) se il token JWT
     * fornito non è valido.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnUnauthorizedWithoutValidToken() throws Exception {

        when(jwtUtil.extractTokenFromHeader("Bearer invalid.token")).thenReturn("invalid.token");
        when(jwtUtil.isTokenValid("invalid.token")).thenReturn(false);


        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", "Bearer invalid.token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token non valido"));

        verify(cartellaService, never()).createCartella(any(), anyString());
    }


    /**
     * Verifica che la creazione di una cartella con dati non validi (es. nome vuoto)
     * restituisca un errore 400 (Bad Request) e i relativi messaggi di validazione.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnBadRequestWithInvalidData() throws Exception {
        CreateCartellaRequest invalidRequest = new CreateCartellaRequest();
        invalidRequest.setNome(""); // Nome vuoto non valido
        invalidRequest.setDescrizione("Descrizione valida");

        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Errori di validazione"))
                .andExpect(jsonPath("$.errors").exists());

        verify(cartellaService, never()).createCartella(any(), anyString());
    }

    /**
     * Verifica che una descrizione superiore a 500 caratteri venga respinta
     * con errore 400 e messaggio di validazione appropriato.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnBadRequestWithTooLongDescription() throws Exception {

        CreateCartellaRequest invalidRequest = new CreateCartellaRequest();
        invalidRequest.setNome("Nome Valido");
        invalidRequest.setDescrizione("a".repeat(501)); // Descrizione troppo lunga (max 500)


        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Errori di validazione"))
                .andExpect(jsonPath("$.errors.descrizione").value("Descrizione deve essere massimo 500 caratteri"));
    }

    /**
     * Verifica la gestione di eccezioni lanciate dal service durante la creazione
     * della cartella, restituendo errore 409 con messaggio custom.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleServiceException() throws Exception {

        when(cartellaService.createCartella(any(CreateCartellaRequest.class), eq(testUsername)))
                .thenThrow(new RuntimeException("Errore generico del servizio"));


        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Errore generico del servizio"));
    }

    /**
     * Verifica la gestione di eccezioni non controllate (es. IllegalStateException),
     * assicurando che venga restituito un errore coerente (409 in questo caso).
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleInternalServerError() throws Exception {
        
        when(cartellaService.createCartella(any(CreateCartellaRequest.class), eq(testUsername)))
                .thenThrow(new IllegalStateException("Errore di stato interno"));


        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest))
                .with(csrf()))
                .andExpect(status().isConflict()) 
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Errore di stato interno"));
    }

    /**
     * Verifica che venga bloccata la creazione di una cartella con nome superiore
     * a 100 caratteri, restituendo errore 400 con messaggio di validazione.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldValidateCartellaNameLength() throws Exception {

        CreateCartellaRequest invalidRequest = new CreateCartellaRequest();
        invalidRequest.setNome("a".repeat(101)); // Nome troppo lungo (max 100)
        invalidRequest.setDescrizione("Descrizione valida");


        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nome").value("Nome cartella deve essere tra 1 e 100 caratteri"));
    }

    /**
     * Verifica che una richiesta GET restituisca correttamente un elenco vuoto
     * se l’utente non ha cartelle.
     */
    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnEmptyCartelleList() throws Exception {

        when(cartellaService.getUserCartelle(testUsername)).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/cartelle")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartelle").isArray())
                .andExpect(jsonPath("$.cartelle", hasSize(0)))
                .andExpect(jsonPath("$.count").value(0));
    }
}