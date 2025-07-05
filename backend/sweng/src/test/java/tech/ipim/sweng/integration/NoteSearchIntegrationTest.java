package tech.ipim.sweng.integration;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.ipim.sweng.dto.RegistrationRequest;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.service.UserService;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class NoteSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    private String authToken;
    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() throws Exception {
        // Crea utenti di test
        RegistrationRequest userRequest = new RegistrationRequest();
        userRequest.setUsername("searchuser");
        userRequest.setPassword("password123");
        userRequest.setEmail("search@test.com");
        
        RegistrationRequest otherUserRequest = new RegistrationRequest();
        otherUserRequest.setUsername("otheruser");
        otherUserRequest.setPassword("password456");
        otherUserRequest.setEmail("other@test.com");

        userService.registerUser(userRequest);
        userService.registerUser(otherUserRequest);

        testUser = userRepository.findByUsername("searchuser").orElseThrow();
        otherUser = userRepository.findByUsername("otheruser").orElseThrow();

        // Genera token di autenticazione (simulazione)
        authToken = "Bearer test-jwt-token";

        // Crea note di test per searchuser
        createTestNote("Nota Java Programming", "Contenuto sui design patterns in Java", 
                      Set.of("Programmazione"), testUser, Set.of("importante", "lavoro"),
                      LocalDateTime.of(2024, 1, 15, 10, 0));

        createTestNote("Algoritmi e Strutture Dati", "Contenuto su algoritmi di ordinamento", 
                      Set.of("Studio"), testUser, Set.of("importante", "studio"),
                      LocalDateTime.of(2024, 2, 10, 14, 0));

        createTestNote("Nota Personale Weekend", "Contenuto sui piani per il weekend", 
                      Set.of("Personale"), testUser, Set.of("personale"),
                      LocalDateTime.of(2024, 3, 5, 9, 0));

        // Crea nota di test per otheruser
        createTestNote("Python Machine Learning", "Contenuto su ML con Python", 
                      Set.of("Programmazione"), otherUser, Set.of("importante"),
                      LocalDateTime.of(2024, 1, 20, 11, 0));

        // Crea nota condivisa
        Note sharedNote = new Note("Nota Condivisa", "Contenuto condiviso con searchuser", otherUser);
        sharedNote.setCartelle(Set.of("Condivisa"));
        sharedNote.setTags(Set.of("condivisa"));
        sharedNote.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA);
        sharedNote.setPermessiLettura(Set.of("searchuser"));
        sharedNote.setDataCreazione(LocalDateTime.of(2024, 1, 25, 15, 0));
        sharedNote.setDataModifica(LocalDateTime.of(2024, 1, 26, 16, 0));
        noteRepository.save(sharedNote);
    }

    private void createTestNote(String titolo, String contenuto, Set<String> cartelle, 
                               User autore, Set<String> tags, LocalDateTime dataCreazione) {
        Note note = new Note(titolo, contenuto, autore);
        note.setCartelle(cartelle);
        note.setTags(tags);
        note.setDataCreazione(dataCreazione);
        note.setDataModifica(dataCreazione.plusHours(1));
        noteRepository.save(note);
    }

    @Test
    void shouldSearchNotesByKeywordSuccessfully() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", authToken)
                .param("q", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo").value("Nota Java Programming"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.keyword").value("Java"));
    }

    @Test
    void shouldSearchNotesByKeywordInContent() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", authToken)
                .param("q", "algoritmi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo").value("Algoritmi e Strutture Dati"))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void shouldSearchNotesIncludingSharedNotes() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", authToken)
                .param("q", "condiviso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo").value("Nota Condivisa"))
                .andExpect(jsonPath("$.notes[0].autore").value("otheruser"));
    }

    @Test
    void shouldGetNotesByTag() throws Exception {
        mockMvc.perform(get("/api/notes/filter/tag/importante")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(3))) // 2 proprie + 1 condivisa
                .andExpect(jsonPath("$.tag").value("importante"));
    }

    @Test
    void shouldGetNotesByCartella() throws Exception {
        mockMvc.perform(get("/api/notes/filter/cartella/Programmazione")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1))) // Solo la nota propria
                .andExpect(jsonPath("$.cartella").value("Programmazione"));
    }

    @Test
    void shouldGetAllNotesWithAllFilter() throws Exception {
        mockMvc.perform(get("/api/notes")
                .header("Authorization", authToken)
                .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(4))); // 3 proprie + 1 condivisa
    }

    @Test
    void shouldGetOwnNotesWithOwnFilter() throws Exception {
        mockMvc.perform(get("/api/notes")
                .header("Authorization", authToken)
                .param("filter", "own"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(3))); // Solo le proprie
    }

    @Test
    void shouldGetNotesWithSearchParam() throws Exception {
        mockMvc.perform(get("/api/notes")
                .header("Authorization", authToken)
                .param("search", "Programming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo").value("Nota Java Programming"));
    }

    @Test
    void shouldGetNotesWithTagParam() throws Exception {
        mockMvc.perform(get("/api/notes")
                .header("Authorization", authToken)
                .param("tag", "studio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo").value("Algoritmi e Strutture Dati"));
    }

    @Test
    void shouldGetNotesWithCartellaParam() throws Exception {
        mockMvc.perform(get("/api/notes")
                .header("Authorization", authToken)
                .param("cartella", "Personale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)));
    }

    @Test
    void shouldGetNotesWithAutoreParam() throws Exception {
        mockMvc.perform(get("/api/notes")
                .header("Authorization", authToken)
                .param("autore", "searchuser")
                .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(3))); // Solo le note di searchuser
    }

    @Test
    void shouldGetNotesWithDateRangeParams() throws Exception {
        mockMvc.perform(get("/api/notes")
                .header("Authorization", authToken)
                .param("dataInizio", "2024-01-01")
                .param("dataFine", "2024-01-31")
                .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(2))); // Note di gennaio
    }

    @Test
    void shouldReturnEmptyResultsWhenNoNotesMatchKeyword() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", authToken)
                .param("q", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(0)))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void shouldHandleCaseInsensitiveSearch() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", authToken)
                .param("q", "JAVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo").value("Nota Java Programming"));
    }

    @Test
    void shouldHandlePartialKeywordSearch() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", authToken)
                .param("q", "Algor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo").value("Algoritmi e Strutture Dati"));
    }

    @Test
    void shouldSearchInBothTitleAndContent() throws Exception {
        // Cerca una parola che appare nel contenuto ma non nel titolo
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", authToken)
                .param("q", "design patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo").value("Nota Java Programming"));
    }

    @Test
    void shouldOnlyReturnUserAccessibleNotesInSearch() throws Exception {
        // Il test verifica che un utente veda solo le proprie note e quelle condivise nei risultati di ricerca
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", authToken)
                .param("q", "Programming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1))) // Solo la nota Java, non quella Python dell'altro utente
                .andExpect(jsonPath("$.notes[0].autore").value("searchuser"));
    }

    @Test
    void shouldGetNoteById() throws Exception {
        // Ottieni l'ID di una nota esistente
        Note existingNote = noteRepository.findByAutoreOrderByDataModificaDesc(testUser).get(0);

        mockMvc.perform(get("/api/notes/" + existingNote.getId())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.note.id").value(existingNote.getId()))
                .andExpect(jsonPath("$.note.autore").value("searchuser"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentNote() throws Exception {
        mockMvc.perform(get("/api/notes/999999")
                .header("Authorization", authToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Nota non trovata o non accessibile"));
    }

    @Test
    void shouldHandleEmptySearchKeyword() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", authToken)
                .param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes").isArray())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void shouldHandleSpecialCharactersInSearch() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", authToken)
                .param("q", "test@#$%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.keyword").value("test@#$%"));
    }
}