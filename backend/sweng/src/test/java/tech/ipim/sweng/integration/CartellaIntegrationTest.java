package tech.ipim.sweng.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tech.ipim.sweng.dto.CreateCartellaRequest;
import tech.ipim.sweng.dto.LoginRequest;
import tech.ipim.sweng.dto.LoginResponse;
import tech.ipim.sweng.dto.UpdateCartellaRequest;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.service.UserService;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CartellaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
    
        testUser = new User("testuser", passwordEncoder.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser = userRepository.save(testUser);

    
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        LoginResponse loginResponse = userService.authenticateUser(loginRequest);
        authToken = "Bearer " + loginResponse.getToken();
    }

    @Test
    @Transactional
    void shouldCreateAndRetrieveCartella() throws Exception {
    
        CreateCartellaRequest createRequest = new CreateCartellaRequest();
        createRequest.setNome("Cartella Test");
        createRequest.setDescrizione("Descrizione cartella test");
        createRequest.setColore("#ff6b6b");

      
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

        
        Long cartellaId = objectMapper.readTree(createResponse).get("cartella").get("id").asLong();

        
        mockMvc.perform(get("/api/cartelle/" + cartellaId)
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartella.id").value(cartellaId))
                .andExpect(jsonPath("$.cartella.nome").value("Cartella Test"));
    }

    @Test
    @Transactional
    void shouldCreateMultipleCartelleAndRetrieveAll() throws Exception {
        
        CreateCartellaRequest cartella1 = new CreateCartellaRequest();
        cartella1.setNome("Lavoro");
        cartella1.setDescrizione("Cartella per note di lavoro");
        cartella1.setColore("#667eea");

        CreateCartellaRequest cartella2 = new CreateCartellaRequest();
        cartella2.setNome("Personale");
        cartella2.setDescrizione("Cartella per note personali");
        cartella2.setColore("#38b2ac");

        
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

        
        mockMvc.perform(get("/api/cartelle")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartelle", hasSize(2)))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.cartelle[*].nome", hasItems("Lavoro", "Personale")));
    }

    @Test
    @Transactional
    void shouldUpdateCartellaSuccessfully() throws Exception {
       
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

    
        mockMvc.perform(get("/api/cartelle/" + cartellaId)
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartella.nome").value("Cartella Aggiornata"));
    }

    @Test
    @Transactional
    void shouldDeleteEmptyCartellaSuccessfully() throws Exception {
        
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

        
        mockMvc.perform(delete("/api/cartelle/" + cartellaId)
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cartella eliminata con successo"));

        
        mockMvc.perform(get("/api/cartelle/" + cartellaId)
                .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void shouldPreventDuplicateCartellaNames() throws Exception {
        
        CreateCartellaRequest createRequest = new CreateCartellaRequest();
        createRequest.setNome("Cartella Unica");
        createRequest.setDescrizione("Prima cartella");

        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        
        CreateCartellaRequest duplicateRequest = new CreateCartellaRequest();
        duplicateRequest.setNome("Cartella Unica");
        duplicateRequest.setDescrizione("Seconda cartella (dovrebbe fallire)");

       
        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Esiste già una cartella con il nome: Cartella Unica"));
    }

    @Test
    @Transactional
    void shouldGetCartelleStatsAfterCreatingCartelle() throws Exception {
        
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

        
        mockMvc.perform(get("/api/cartelle/stats")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.numeroCartelle").value(3))
                .andExpect(jsonPath("$.stats.nomiCartelle", hasSize(3)))
                .andExpect(jsonPath("$.stats.nomiCartelle", hasItems("Lavoro", "Personale", "Studio")));
    }

    @Test
    void shouldRejectRequestWithoutAuthentication() throws Exception {
       
        CreateCartellaRequest request = new CreateCartellaRequest();
        request.setNome("Cartella Non Autorizzata");
        request.setDescrizione("Questa dovrebbe fallire");

        
        mockMvc.perform(post("/api/cartelle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); 
    }

    @Test
    void shouldRejectInvalidCartellaData() throws Exception {

        CreateCartellaRequest invalidRequest = new CreateCartellaRequest();
        invalidRequest.setNome(""); 
        invalidRequest.setDescrizione("a".repeat(501)); // Descrizione troppo lunga

       
        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @Transactional
    void shouldAllowSameCartellaNameForDifferentUsers() throws Exception {
       
        User secondUser = new User("seconduser", passwordEncoder.encode("password456"));
        secondUser.setEmail("second@example.com");
        secondUser = userRepository.save(secondUser);

        LoginRequest secondLoginRequest = new LoginRequest("seconduser", "password456");
        LoginResponse secondLoginResponse = userService.authenticateUser(secondLoginRequest);
        String secondAuthToken = "Bearer " + secondLoginResponse.getToken();

        
        CreateCartellaRequest request1 = new CreateCartellaRequest();
        request1.setNome("Cartella Condivisa");
        request1.setDescrizione("Cartella del primo utente");

        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        
        CreateCartellaRequest request2 = new CreateCartellaRequest();
        request2.setNome("Cartella Condivisa");
        request2.setDescrizione("Cartella del secondo utente");

        
        mockMvc.perform(post("/api/cartelle")
                .header("Authorization", secondAuthToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Transactional
    void shouldNotAllowAccessToOtherUsersCartelle() throws Exception {

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

        
        mockMvc.perform(get("/api/cartelle/" + cartellaId)
                .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }
}