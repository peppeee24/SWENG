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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.LoginRequest;
import tech.ipim.sweng.dto.LoginResponse;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.service.UserService;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NoteIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

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
        // Configura MockMvc con il contesto di sicurezza
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Crea e salva un utente di test
        testUser = new User("testuser", passwordEncoder.encode("password123"));
        testUser.setEmail("test@example.com");
        testUser = userRepository.save(testUser);

        // Effettua login per ottenere il token
        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
        LoginResponse loginResponse = userService.authenticateUser(loginRequest);
        authToken = "Bearer " + loginResponse.getToken();
    }

    @Test
    @Transactional
    void shouldCreateAndRetrieveNote() throws Exception {
        // Given - Crea una nuova nota
        CreateNoteRequest createRequest = new CreateNoteRequest();
        createRequest.setTitolo("Integration Test Note");
        createRequest.setContenuto("This is a test note for integration testing");
        createRequest.setTags(Set.of("integration", "test"));
        createRequest.setCartelle(Set.of("Test Folder"));

        // When - Crea la nota
        String createResponse = mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.note.titolo").value("Integration Test Note"))
                .andExpect(jsonPath("$.note.contenuto").value("This is a test note for integration testing"))
                .andExpect(jsonPath("$.note.autore").value("testuser"))
                .andExpect(jsonPath("$.note.tags", hasItem("integration")))
                .andExpect(jsonPath("$.note.cartelle", hasItem("Test Folder")))
                .andReturn().getResponse().getContentAsString();

        // Extract note ID from response
        Long noteId = objectMapper.readTree(createResponse).get("note").get("id").asLong();

        // Then - Recupera la nota appena creata
        mockMvc.perform(get("/api/notes/" + noteId)
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.note.id").value(noteId))
                .andExpect(jsonPath("$.note.titolo").value("Integration Test Note"));
    }

    @Test
    @Transactional
    void shouldCreateMultipleNotesAndRetrieveAll() throws Exception {
        // Given - Crea multiple note
        CreateNoteRequest note1 = new CreateNoteRequest();
        note1.setTitolo("First Note");
        note1.setContenuto("Content of first note");
        note1.setTags(Set.of("first", "test"));

        CreateNoteRequest note2 = new CreateNoteRequest();
        note2.setTitolo("Second Note");
        note2.setContenuto("Content of second note");
        note2.setTags(Set.of("second", "test"));

        // When - Crea entrambe le note
        mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(note1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(note2)))
                .andExpect(status().isCreated());

        // Then - Recupera tutte le note
        mockMvc.perform(get("/api/notes")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(2)))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.notes[*].titolo", hasItems("First Note", "Second Note")));
    }

    @Test
    @Transactional
    void shouldSearchNotesSuccessfully() throws Exception {
        // Given - Crea note con contenuti diversi
        CreateNoteRequest searchableNote = new CreateNoteRequest();
        searchableNote.setTitolo("Searchable Note");
        searchableNote.setContenuto("This note contains searchable content");

        CreateNoteRequest otherNote = new CreateNoteRequest();
        otherNote.setTitolo("Other Note");
        otherNote.setContenuto("This is different content");

        mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchableNote)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otherNote)))
                .andExpect(status().isCreated());

        // When & Then - Cerca per parola chiave
        mockMvc.perform(get("/api/notes/search?q=searchable")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo").value("Searchable Note"))
                .andExpect(jsonPath("$.keyword").value("searchable"));
    }

    @Test
    @Transactional
    void shouldFilterNotesByTag() throws Exception {
        // Given - Crea note con tag diversi
        CreateNoteRequest workNote = new CreateNoteRequest();
        workNote.setTitolo("Work Note");
        workNote.setContenuto("Work related content");
        workNote.setTags(Set.of("work", "important"));

        CreateNoteRequest personalNote = new CreateNoteRequest();
        personalNote.setTitolo("Personal Note");
        personalNote.setContenuto("Personal content");
        personalNote.setTags(Set.of("personal", "hobby"));

        mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(workNote)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(personalNote)))
                .andExpect(status().isCreated());

        // When & Then - Filtra per tag "work"
        mockMvc.perform(get("/api/notes/filter/tag/work")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo").value("Work Note"))
                .andExpect(jsonPath("$.tag").value("work"));
    }

    @Test
    @Transactional
    void shouldDuplicateNoteSuccessfully() throws Exception {
        // Given - Crea una nota
        CreateNoteRequest originalNote = new CreateNoteRequest();
        originalNote.setTitolo("Original Note");
        originalNote.setContenuto("Original content");
        originalNote.setTags(Set.of("original", "test"));

        String createResponse = mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(originalNote)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long noteId = objectMapper.readTree(createResponse).get("note").get("id").asLong();

        // When - Duplica la nota
        mockMvc.perform(post("/api/notes/" + noteId + "/duplicate")
                .header("Authorization", authToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.note.titolo").value("Original Note (Copia)"))
                .andExpect(jsonPath("$.note.contenuto").value("Original content"))
                .andExpect(jsonPath("$.note.tags", hasItem("original")));

        // Then - Verifica che ci siano ora 2 note
        mockMvc.perform(get("/api/notes")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", hasSize(2)));
    }

    @Test
    @Transactional
    void shouldDeleteNoteSuccessfully() throws Exception {
        // Given - Crea una nota
        CreateNoteRequest noteToDelete = new CreateNoteRequest();
        noteToDelete.setTitolo("Note to Delete");
        noteToDelete.setContenuto("This note will be deleted");

        String createResponse = mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(noteToDelete)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long noteId = objectMapper.readTree(createResponse).get("note").get("id").asLong();

        // When - Elimina la nota
        mockMvc.perform(delete("/api/notes/" + noteId)
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nota eliminata con successo"));

        // Then - Verifica che la nota non esista più
        mockMvc.perform(get("/api/notes/" + noteId)
                .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void shouldGetUserStatsAfterCreatingNotes() throws Exception {
        // Given - Crea alcune note con tag e cartelle
        CreateNoteRequest note1 = new CreateNoteRequest();
        note1.setTitolo("Note 1");
        note1.setContenuto("Content 1");
        note1.setTags(Set.of("tag1", "tag2"));
        note1.setCartelle(Set.of("folder1"));

        CreateNoteRequest note2 = new CreateNoteRequest();
        note2.setTitolo("Note 2");
        note2.setContenuto("Content 2");
        note2.setTags(Set.of("tag2", "tag3"));
        note2.setCartelle(Set.of("folder1", "folder2"));

        mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(note1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(note2)))
                .andExpect(status().isCreated());

        // When & Then - Verifica le statistiche
        mockMvc.perform(get("/api/notes/stats")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.noteCreate").value(2))
                .andExpect(jsonPath("$.stats.noteCondivise").value(0))
                .andExpect(jsonPath("$.stats.tagUtilizzati").value(3))
                .andExpect(jsonPath("$.stats.cartelleCreate").value(2))
                .andExpect(jsonPath("$.stats.allTags", hasSize(3)))
                .andExpect(jsonPath("$.stats.allCartelle", hasSize(2)));
    }

    @Test
    void shouldRejectRequestWithoutAuthentication() throws Exception {
        // Given - Richiesta senza token di autenticazione
        CreateNoteRequest request = new CreateNoteRequest();
        request.setTitolo("Unauthorized Note");
        request.setContenuto("This should fail");

        // When & Then
        mockMvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // Spring Security response
    }

    @Test
    void shouldRejectInvalidNoteData() throws Exception {
        // Given - Richiesta con dati non validi
        CreateNoteRequest invalidRequest = new CreateNoteRequest();
        invalidRequest.setTitolo(""); // Titolo vuoto
        invalidRequest.setContenuto("a".repeat(281)); // Contenuto troppo lungo

        // When & Then
        mockMvc.perform(post("/api/notes")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").exists());
    }
}