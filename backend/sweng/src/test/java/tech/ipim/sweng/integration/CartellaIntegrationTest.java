package tech.ipim.sweng.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tech.ipim.sweng.dto.CreateCartellaRequest;
import tech.ipim.sweng.dto.LoginRequest;
import tech.ipim.sweng.dto.LoginResponse;
import tech.ipim.sweng.dto.UpdateCartellaRequest;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.service.UserService;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;


/**
 * Test di integrazione end-to-end per {@link CartellaController}, eseguiti in ambiente
 * {@link SpringBootTest} con database reale e contesto applicativo completo.
 * <p>
 * Verifica il funzionamento completo delle API REST per la gestione delle cartelle,
 * includendo persistenza dei dati, autenticazione JWT, validazione e sicurezza.
 * Ogni test utilizza transazioni isolate e pulizia del contesto per garantire indipendenza.
 * <p>
 * I test simulano scenari reali di utilizzo con utenti autenticati, validando
 * il comportamento end-to-end del sistema dalla richiesta HTTP alla persistenza nel database.
 * <p>
 * <p>
 * Riepilogo dei test implementati:
 * <ul>
 *   <li>{@code shouldCreateAndRetrieveCartella} – Ciclo completo creazione e recupero cartella</li>
 *   <li>{@code shouldCreateMultipleCartelleAndRetrieveAll} – Creazione e recupero multiple cartelle</li>
 *   <li>{@code shouldUpdateCartellaSuccessfully} – Aggiornamento completo cartella con persistenza</li>
 *   <li>{@code shouldDeleteEmptyCartellaSuccessfully} – Eliminazione cartella vuota e verifica persistenza</li>
 *   <li>{@code shouldPreventDuplicateCartellaNames} – Prevenzione duplicati nomi cartelle per stesso utente</li>
 *   <li>{@code shouldGetCartelleStatsAfterCreatingCartelle} – Statistiche cartelle dopo creazione multiple</li>
 *   <li>{@code shouldRejectRequestWithoutAuthentication} – Rifiuto richieste senza autenticazione</li>
 *   <li>{@code shouldRejectInvalidCartellaData} – Validazione dati cartella e gestione errori</li>
 *   <li>{@code shouldAllowSameCartellaNameForDifferentUsers} – Permesso stesso nome per utenti diversi</li>
 *   <li>{@code shouldNotAllowAccessToOtherUsersCartelle} – Isolamento cartelle tra utenti diversi</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CartellaIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @LocalServerPort
    private int port;

    private MockMvc mockMvc;
    private String authToken;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        // Configura MockMvc con il contesto web e la sicurezza
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Crea utente di test
        testUser = new User("testuser", passwordEncoder.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser = userRepository.save(testUser);

        // Ottieni token di autenticazione
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        LoginResponse loginResponse = userService.authenticateUser(loginRequest);
        authToken = "Bearer " + loginResponse.getToken();
    }


    /**
     * Verifica il ciclo completo di creazione e recupero di una cartella,
     * testando persistenza dei dati e corretta serializzazione JSON.
     *
     * Esegue:
     * 1. Creazione cartella tramite POST con dati validi
     * 2. Estrazione ID dalla risposta JSON
     * 3. Recupero cartella tramite GET utilizzando l'ID
     * 4. Verifica persistenza e coerenza dei dati
     */
    @Test
    @Transactional
    void shouldCreateAndRetrieveCartella() throws Exception {
        // Crea una nuova cartella
        CreateCartellaRequest createRequest = new CreateCartellaRequest();
        createRequest.setNome("Cartella Test");
        createRequest.setDescrizione("Descrizione cartella test");
        createRequest.setColore("#ff6b6b");

        // Esegui la richiesta di creazione
        String createResponse = mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartella.nome").value("Cartella Test"))
                .andExpect(jsonPath("$.cartella.descrizione").value("Descrizione cartella test"))
                .andExpect(jsonPath("$.cartella.colore").value("#ff6b6b"))
                .andExpect(jsonPath("$.cartella.proprietario").value("testuser"))
                .andExpect(jsonPath("$.cartella.numeroNote").value(0))
                .andReturn().getResponse().getContentAsString();

        // Estrai l'ID della cartella creata
        Long cartellaId = objectMapper.readTree(createResponse).get("cartella").get("id").asLong();

        // Verifica che la cartella possa essere recuperata
        mockMvc.perform(get("/api/cartelle/" + cartellaId)
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartella.id").value(cartellaId))
                .andExpect(jsonPath("$.cartella.nome").value("Cartella Test"));
    }


    /**
     * Verifica la creazione di multiple cartelle e il loro recupero
     * in un'unica chiamata, testando la gestione di liste e contatori.
     *
     * Testa la capacità del sistema di gestire più cartelle per lo stesso
     * utente e la corretta restituzione di tutte le cartelle associate.
     */
    @Test
    @Transactional
    void shouldCreateMultipleCartelleAndRetrieveAll() throws Exception {
        // Crea prima cartella
        CreateCartellaRequest cartella1 = new CreateCartellaRequest();
        cartella1.setNome("Lavoro");
        cartella1.setDescrizione("Cartella per note di lavoro");
        cartella1.setColore("#667eea");

        CreateCartellaRequest cartella2 = new CreateCartellaRequest();
        cartella2.setNome("Personale");
        cartella2.setDescrizione("Cartella per note personali");
        cartella2.setColore("#38b2ac");

        // Crea entrambe le cartelle
        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cartella1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cartella2)))
                .andExpect(status().isCreated());

        // Verifica che entrambe le cartelle vengano recuperate
        mockMvc.perform(get("/api/cartelle")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartelle", hasSize(2)))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.cartelle[*].nome", hasItems("Lavoro", "Personale")));
    }


    /**
     * Verifica il ciclo completo di aggiornamento di una cartella esistente,
     * inclusa la persistenza delle modifiche nel database.
     *
     * Esegue:
     * 1. Creazione cartella iniziale
     * 2. Aggiornamento con nuovi dati tramite PUT
     * 3. Verifica immediata della risposta di aggiornamento
     * 4. Verifica persistenza tramite GET successivo
     */
    @Test
    @Transactional
    void shouldUpdateCartellaSuccessfully() throws Exception {
        // Crea cartella iniziale
        CreateCartellaRequest createRequest = new CreateCartellaRequest();
        createRequest.setNome("Cartella Originale");
        createRequest.setDescrizione("Descrizione originale");
        createRequest.setColore("#ff6b6b");

        String createResponse = mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long cartellaId = objectMapper.readTree(createResponse).get("cartella").get("id").asLong();

        // Aggiorna la cartella
        UpdateCartellaRequest updateRequest = new UpdateCartellaRequest();
        updateRequest.setNome("Cartella Aggiornata");
        updateRequest.setDescrizione("Descrizione aggiornata");
        updateRequest.setColore("#4ecdc4");

        mockMvc.perform(put("/api/cartelle/" + cartellaId)
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartella.nome").value("Cartella Aggiornata"))
                .andExpect(jsonPath("$.cartella.descrizione").value("Descrizione aggiornata"))
                .andExpect(jsonPath("$.cartella.colore").value("#4ecdc4"));

        // Verifica che l'aggiornamento sia persistito
        mockMvc.perform(get("/api/cartelle/" + cartellaId)
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartella.nome").value("Cartella Aggiornata"));
    }


    /**
     * Verifica l'eliminazione di una cartella vuota e la corretta
     * rimozione dalla persistenza del database.
     *
     * Testa che dopo l'eliminazione la cartella non sia più
     * accessibile e restituisca errore 404 Not Found.
     */
    @Test
    @Transactional
    void shouldDeleteEmptyCartellaSuccessfully() throws Exception {
        // Crea cartella da eliminare
        CreateCartellaRequest createRequest = new CreateCartellaRequest();
        createRequest.setNome("Cartella da Eliminare");
        createRequest.setDescrizione("Questa cartella sarà eliminata");

        String createResponse = mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long cartellaId = objectMapper.readTree(createResponse).get("cartella").get("id").asLong();

        // Elimina la cartella
        mockMvc.perform(delete("/api/cartelle/" + cartellaId)
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cartella eliminata con successo"));

        // Verifica che la cartella non esista più
        mockMvc.perform(get("/api/cartelle/" + cartellaId)
                .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }


    /**
     * Verifica che il sistema impedisca la creazione di cartelle
     * con nomi duplicati per lo stesso utente.
     *
     * Controlla che venga restituito errore 409 Conflict quando
     * si tenta di creare una seconda cartella con nome già esistente.
     */
    @Test
    @Transactional
    void shouldPreventDuplicateCartellaNames() throws Exception {
        // Crea prima cartella
        CreateCartellaRequest createRequest = new CreateCartellaRequest();
        createRequest.setNome("Cartella Unica");
        createRequest.setDescrizione("Prima cartella");

        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Tenta di creare cartella con stesso nome
        CreateCartellaRequest duplicateRequest = new CreateCartellaRequest();
        duplicateRequest.setNome("Cartella Unica");
        duplicateRequest.setDescrizione("Seconda cartella (dovrebbe fallire)");

        // Verifica che la creazione fallisca
        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Esiste già una cartella con il nome: Cartella Unica"));
    }


    /**
     * Verifica che le statistiche delle cartelle vengano aggiornate
     * correttamente dopo la creazione di multiple cartelle.
     *
     * Testa l'endpoint di statistiche e la corretta contabilizzazione
     * del numero di cartelle e dei loro nomi.
     */
    @Test
    @Transactional
    void shouldGetCartelleStatsAfterCreatingCartelle() throws Exception {
        // Crea multiple cartelle
        String[] nomiCartelle = {"Lavoro", "Personale", "Studio"};
        
        for (String nome : nomiCartelle) {
            CreateCartellaRequest request = new CreateCartellaRequest();
            request.setNome(nome);
            request.setDescrizione("Descrizione per " + nome);

            mockMvc.perform(post("/api/cartelle")
                    .header("Authorization", authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Verifica le statistiche
        mockMvc.perform(get("/api/cartelle/stats")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.numeroCartelle").value(3))
                .andExpect(jsonPath("$.stats.nomiCartelle", hasSize(3)))
                .andExpect(jsonPath("$.stats.nomiCartelle", hasItems("Lavoro", "Personale", "Studio")));
    }


    /**
     * Verifica che le richieste senza token di autenticazione
     * vengano respinte con errore di autorizzazione appropriato.
     *
     * Testa la sicurezza dell'endpoint assicurando che solo
     * utenti autenticati possano accedere alle funzionalità.
     */
    @Test
    void shouldRejectRequestWithoutAuthentication() throws Exception {
        // Tenta di creare cartella senza autenticazione
        CreateCartellaRequest request = new CreateCartellaRequest();
        request.setNome("Cartella Non Autorizzata");
        request.setDescrizione("Questa dovrebbe fallire");

        // Verifica che la richiesta venga rifiutata
        mockMvc.perform(post("/api/cartelle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // O isUnauthorized() a seconda della configurazione
    }


    /**
     * Verifica che la validazione dei dati cartella funzioni correttamente,
     * respingendo richieste con dati non validi.
     *
     * Testa scenari come nome vuoto e descrizione troppo lunga,
     * verificando che vengano restituiti errori 400 Bad Request.
     */
    @Test
    void shouldRejectInvalidCartellaData() throws Exception {
        // Crea richiesta con dati non validi
        CreateCartellaRequest invalidRequest = new CreateCartellaRequest();
        invalidRequest.setNome(""); // Nome vuoto
        invalidRequest.setDescrizione("a".repeat(501)); // Descrizione troppo lunga

        // Verifica che la validazione fallisca
        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());
    }


    /**
     * Verifica che utenti diversi possano creare cartelle con lo stesso nome
     * senza conflitti, mantenendo l'isolamento tra utenti.
     *
     * Testa che la validazione dei nomi duplicati sia applicata
     * solo all'interno dello scope del singolo utente.
     */
    @Test
    @Transactional
    void shouldAllowSameCartellaNameForDifferentUsers() throws Exception {
        // Crea secondo utente
        User secondUser = new User("seconduser", passwordEncoder.encode("password456"));
        secondUser.setEmail("second@example.com");
        secondUser = userRepository.save(secondUser);

        LoginRequest secondLoginRequest = new LoginRequest("seconduser", "password456");
        LoginResponse secondLoginResponse = userService.authenticateUser(secondLoginRequest);
        String secondAuthToken = "Bearer " + secondLoginResponse.getToken();

        // Crea cartella per primo utente
        CreateCartellaRequest request1 = new CreateCartellaRequest();
        request1.setNome("Cartella Condivisa");
        request1.setDescrizione("Cartella del primo utente");

        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Crea cartella con stesso nome per secondo utente
        CreateCartellaRequest request2 = new CreateCartellaRequest();
        request2.setNome("Cartella Condivisa");
        request2.setDescrizione("Cartella del secondo utente");

        // Verifica che sia consentito
        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", secondAuthToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }


    /**
     * Verifica l'isolamento di sicurezza tra utenti diversi,
     * assicurando che un utente non possa accedere alle cartelle di altri.
     *
     * Testa che il tentativo di accesso a cartelle di altri utenti
     * restituisca errore 404 Not Found, mantenendo la privacy dei dati.
     */
    @Test
    @Transactional
    void shouldNotAllowAccessToOtherUsersCartelle() throws Exception {
        // Crea secondo utente
        User secondUser = new User("seconduser", passwordEncoder.encode("password456"));
        secondUser.setEmail("second@example.com");
        secondUser = userRepository.save(secondUser);

        LoginRequest secondLoginRequest = new LoginRequest("seconduser", "password456");
        LoginResponse secondLoginResponse = userService.authenticateUser(secondLoginRequest);
        String secondAuthToken = "Bearer " + secondLoginResponse.getToken();

        CreateCartellaRequest request = new CreateCartellaRequest();
        request.setNome("Cartella Privata");
        request.setDescrizione("Cartella del secondo utente");

        String createResponse = mockMvc.perform(post("/api/cartelle")
                .header("Authorization", secondAuthToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long cartellaId = objectMapper.readTree(createResponse).get("cartella").get("id").asLong();

        // Verifica che il primo utente non possa accedere alla cartella del secondo
        mockMvc.perform(get("/api/cartelle/" + cartellaId)
                .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }
}