package tech.ipim.sweng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tech.ipim.sweng.config.TestConfig;
import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.NoteDto;
import tech.ipim.sweng.service.NoteService;
import tech.ipim.sweng.util.JwtUtil;
import tech.ipim.sweng.dto.UpdateNoteRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoteController.class)
@Import(TestConfig.class)
@ActiveProfiles("test")
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NoteService noteService;

    @MockBean
    private JwtUtil jwtUtil;

    private NoteDto testNoteDto;
    private CreateNoteRequest createRequest;
    private final String validToken = "Bearer valid.jwt.token";
    private final String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        testNoteDto = new NoteDto();
        testNoteDto.setId(1L);
        testNoteDto.setTitolo("Test Note");
        testNoteDto.setContenuto("Test content");
        testNoteDto.setAutore(testUsername);
        testNoteDto.setDataCreazione(LocalDateTime.now());
        testNoteDto.setDataModifica(LocalDateTime.now());
        testNoteDto.setTags(Set.of("test", "sample"));
        testNoteDto.setCartelle(Set.of("Test Folder"));
        testNoteDto.setCanEdit(true);
        testNoteDto.setCanDelete(true);

        createRequest = new CreateNoteRequest();
        createRequest.setTitolo("New Note");
        createRequest.setContenuto("New note content");
        createRequest.setTags(Set.of("new", "test"));

        when(jwtUtil.extractTokenFromHeader(validToken)).thenReturn("valid.jwt.token");
        when(jwtUtil.isTokenValid("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid.jwt.token")).thenReturn(testUsername);

        // Mock per tutti i token usati nei test di update
        when(jwtUtil.extractTokenFromHeader("Bearer valid-jwt-token")).thenReturn("valid-jwt-token");
        when(jwtUtil.isTokenValid("valid-jwt-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-jwt-token")).thenReturn(testUsername);

        when(jwtUtil.extractTokenFromHeader("Bearer invalid-token")).thenReturn("invalid-token");
        when(jwtUtil.isTokenValid("invalid-token")).thenReturn(false);
        when(jwtUtil.extractUsername("invalid-token")).thenReturn(null);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldCreateNoteSuccessfully() throws Exception {
        when(noteService.createNote(any(CreateNoteRequest.class), eq(testUsername))).thenReturn(testNoteDto);

        mockMvc.perform(post("/api/notes")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nota creata con successo"))
                .andExpect(jsonPath("$.note.id").value(1))
                .andExpect(jsonPath("$.note.titolo").value("Test Note"))
                .andExpect(jsonPath("$.note.contenuto").value("Test content"));

        verify(noteService).createNote(any(CreateNoteRequest.class), eq(testUsername));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetUserStats() throws Exception {
        NoteService.UserNotesStats stats = new NoteService.UserNotesStats(
                5L, 3L, 4L, 2L,
                Arrays.asList("tag1", "tag2", "tag3", "tag4"),
                Arrays.asList("folder1", "folder2")
        );
        when(noteService.getUserStats(testUsername)).thenReturn(stats);

        mockMvc.perform(get("/api/notes/stats")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.noteCreate").value(5))
                .andExpect(jsonPath("$.stats.noteCondivise").value(3))
                .andExpect(jsonPath("$.stats.tagUtilizzati").value(4))
                .andExpect(jsonPath("$.stats.cartelleCreate").value(2))
                .andExpect(jsonPath("$.stats.allTags", hasSize(4)))
                .andExpect(jsonPath("$.stats.allCartelle", hasSize(2)));

        verify(noteService).getUserStats(testUsername);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleServiceException() throws Exception {
        when(noteService.createNote(any(CreateNoteRequest.class), eq(testUsername)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/api/notes")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Errore durante la creazione della nota"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetAllNotesSuccessfully() throws Exception {
        List<NoteDto> notes = Arrays.asList(testNoteDto);
        when(noteService.getAllAccessibleNotes(testUsername)).thenReturn(notes);

        mockMvc.perform(get("/api/notes")
                        .header("Authorization", validToken))
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes").isArray())
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.notes[0].titolo").value("Test Note"));

        verify(noteService).getAllAccessibleNotes(testUsername);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldDeleteNote() throws Exception {
        when(noteService.deleteNote(1L, testUsername)).thenReturn(true);

        mockMvc.perform(delete("/api/notes/1")
                        .header("Authorization", validToken)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nota eliminata con successo"));

        verify(noteService).deleteNote(1L, testUsername);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldDuplicateNote() throws Exception {
        NoteDto duplicatedNote = new NoteDto();
        duplicatedNote.setId(2L);
        duplicatedNote.setTitolo("Test Note (Copia)");
        duplicatedNote.setContenuto("Test content");
        duplicatedNote.setAutore(testUsername);

        when(noteService.duplicateNote(1L, testUsername)).thenReturn(duplicatedNote);

        mockMvc.perform(post("/api/notes/1/duplicate")
                        .header("Authorization", validToken)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nota duplicata con successo"))
                .andExpect(jsonPath("$.note.id").value(2))
                .andExpect(jsonPath("$.note.titolo").value("Test Note (Copia)"));

        verify(noteService).duplicateNote(1L, testUsername);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldSearchNotes() throws Exception {
        when(noteService.searchNotes(testUsername, "test")).thenReturn(Arrays.asList(testNoteDto));

        mockMvc.perform(get("/api/notes/search?q=test")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.keyword").value("test"));

        verify(noteService).searchNotes(testUsername, "test");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetNoteById() throws Exception {
        when(noteService.getNoteById(1L, testUsername)).thenReturn(Optional.of(testNoteDto));

        mockMvc.perform(get("/api/notes/1")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.note.id").value(1))
                .andExpect(jsonPath("$.note.titolo").value("Test Note"));

        verify(noteService).getNoteById(1L, testUsername);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldFilterNotesByTag() throws Exception {
        when(noteService.getNotesByTag(testUsername, "test")).thenReturn(Arrays.asList(testNoteDto));

        mockMvc.perform(get("/api/notes/filter/tag/test")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tag").value("test"));

        verify(noteService).getNotesByTag(testUsername, "test");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldFilterNotesByCartella() throws Exception {
        when(noteService.getNotesByCartella(testUsername, "Test Folder")).thenReturn(Arrays.asList(testNoteDto));

        mockMvc.perform(get("/api/notes/filter/cartella/Test Folder")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartella").value("Test Folder"));

        verify(noteService).getNotesByCartella(testUsername, "Test Folder");
    }

    @Test
    @DisplayName("PUT /api/notes/{id} - Dovrebbe aggiornare una nota con successo")
    void shouldUpdateNoteSuccessfully() throws Exception {
        // Arrange
        String token = "Bearer valid-jwt-token";
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Titolo Aggiornato");
        request.setContenuto("Contenuto aggiornato");
        request.setTags(Set.of("nuovo-tag"));
        request.setCartelle(Set.of("nuova-cartella"));

        NoteDto updatedNote = new NoteDto();
        updatedNote.setId(1L);
        updatedNote.setTitolo("Titolo Aggiornato");
        updatedNote.setContenuto("Contenuto aggiornato");
        updatedNote.setAutore("testuser");

        when(noteService.updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser")))
                .thenReturn(updatedNote);

        // Act & Assert
        mockMvc.perform(put("/api/notes/1")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nota aggiornata con successo"))
                .andExpect(jsonPath("$.note.id").value(1))
                .andExpect(jsonPath("$.note.titolo").value("Titolo Aggiornato"))
                .andExpect(jsonPath("$.note.contenuto").value("Contenuto aggiornato"));

        verify(noteService).updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser"));
    }

    @Test
    @DisplayName("PUT /api/notes/{id} - Dovrebbe fallire con token non valido")
    void shouldFailUpdateWithInvalidToken() throws Exception {
        // Arrange
        String invalidToken = "Bearer invalid-token";
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Test");
        request.setContenuto("Test content");

        // Act & Assert
        mockMvc.perform(put("/api/notes/1")
                        .header("Authorization", invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token non valido"));

        verify(noteService, never()).updateNote(anyLong(), any(UpdateNoteRequest.class), anyString());
    }

    @Test
    @DisplayName("PUT /api/notes/{id} - Dovrebbe fallire con dati di validazione errati")
    void shouldFailUpdateWithValidationErrors() throws Exception {
        // Arrange
        String token = "Bearer valid-jwt-token";
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo(""); // Titolo vuoto - errore di validazione
        request.setContenuto("Contenuto valido");

        // Act & Assert
        mockMvc.perform(put("/api/notes/1")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Errori di validazione"))
                .andExpect(jsonPath("$.errors").exists());

        verify(noteService, never()).updateNote(anyLong(), any(UpdateNoteRequest.class), anyString());
    }

    @Test
    @DisplayName("PUT /api/notes/{id} - Dovrebbe fallire quando l'utente non ha permessi")
    void shouldFailUpdateWhenUserHasNoPermission() throws Exception {
        // Arrange
        String token = "Bearer valid-jwt-token";
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Tentativo Modifica");
        request.setContenuto("Non autorizzato");

        when(noteService.updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser")))
                .thenThrow(new RuntimeException("Non hai i permessi per modificare questa nota"));

        // Act & Assert
        mockMvc.perform(put("/api/notes/1")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Non hai i permessi per modificare questa nota"));
    }

    @Test
    @DisplayName("PUT /api/notes/{id} - Dovrebbe fallire quando la nota non esiste")
    void shouldFailUpdateWhenNoteNotFound() throws Exception {
        // Arrange
        String token = "Bearer valid-jwt-token";
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Nota Inesistente");
        request.setContenuto("Non esiste");

        when(noteService.updateNote(eq(999L), any(UpdateNoteRequest.class), eq("testuser")))
                .thenThrow(new RuntimeException("Nota non trovata"));

        // Act & Assert
        mockMvc.perform(put("/api/notes/999")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Nota non trovata"));
    }

    @Test
    @DisplayName("PUT /api/notes/{id} - Dovrebbe gestire errori interni del server")
    void shouldHandleInternalServerErrorForUpdate() throws Exception {
        // Arrange
        String token = "Bearer valid-jwt-token";
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Test");
        request.setContenuto("Test content");

        when(noteService.updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser")))
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        mockMvc.perform(put("/api/notes/1")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Database connection error"));
    }

    @Test
    @DisplayName("PUT /api/notes/{id} - Dovrebbe aggiornare solo titolo e contenuto")
    void shouldUpdateOnlyTitleAndContent() throws Exception {
        // Arrange
        String token = "Bearer valid-jwt-token";
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Solo Titolo");
        request.setContenuto("Solo Contenuto");
        // tags e cartelle non specificati (null)

        NoteDto updatedNote = new NoteDto();
        updatedNote.setId(1L);
        updatedNote.setTitolo("Solo Titolo");
        updatedNote.setContenuto("Solo Contenuto");

        when(noteService.updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser")))
                .thenReturn(updatedNote);

        // Act & Assert
        mockMvc.perform(put("/api/notes/1")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.note.titolo").value("Solo Titolo"))
                .andExpect(jsonPath("$.note.contenuto").value("Solo Contenuto"));

        verify(noteService).updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser"));
    }

    @Test
    @DisplayName("PUT /api/notes/{id} - Dovrebbe fallire senza header Authorization")
    void shouldFailUpdateWithoutAuthorizationHeader() throws Exception {
        // Arrange
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Test");
        request.setContenuto("Test content");

        // Act & Assert - Spring restituisce 400 quando un @RequestHeader required manca
        mockMvc.perform(put("/api/notes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest()); // 400, non 401

        verify(noteService, never()).updateNote(anyLong(), any(UpdateNoteRequest.class), anyString());
    }
}