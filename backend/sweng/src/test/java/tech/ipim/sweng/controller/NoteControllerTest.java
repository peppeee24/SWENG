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
import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.NoteDto;
import tech.ipim.sweng.service.NoteService;
import tech.ipim.sweng.util.JwtUtil;

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

        // Mock JWT utility
        when(jwtUtil.extractTokenFromHeader(validToken)).thenReturn("valid.jwt.token");
        when(jwtUtil.isTokenValid("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid.jwt.token")).thenReturn(testUsername);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldCreateNoteSuccessfully() throws Exception {
        // Given
        when(noteService.createNote(any(CreateNoteRequest.class), eq(testUsername))).thenReturn(testNoteDto);

        // When & Then
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
        // Given
        NoteService.UserNotesStats stats = new NoteService.UserNotesStats(
                5L, 3L, 4L, 2L,
                Arrays.asList("tag1", "tag2", "tag3", "tag4"),
                Arrays.asList("folder1", "folder2")
        );
        when(noteService.getUserStats(testUsername)).thenReturn(stats);

        // When & Then
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
        // Given
        when(noteService.createNote(any(CreateNoteRequest.class), eq(testUsername)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
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
        // Given
        List<NoteDto> notes = Arrays.asList(testNoteDto);
        when(noteService.getAllAccessibleNotes(testUsername)).thenReturn(notes);

        // When & Then
        mockMvc.perform(get("/api/notes")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
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
        // Given
        when(noteService.deleteNote(1L, testUsername)).thenReturn(true);

        // When & Then
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
        // Given
        NoteDto duplicatedNote = new NoteDto();
        duplicatedNote.setId(2L);
        duplicatedNote.setTitolo("Test Note (Copia)");
        duplicatedNote.setContenuto("Test content");
        duplicatedNote.setAutore(testUsername);

        when(noteService.duplicateNote(1L, testUsername)).thenReturn(duplicatedNote);

        // When & Then
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
        // Given
        when(noteService.searchNotes(testUsername, "test")).thenReturn(Arrays.asList(testNoteDto));

        // When & Then
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
        // Given
        when(noteService.getNoteById(1L, testUsername)).thenReturn(Optional.of(testNoteDto));

        // When & Then
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
        // Given
        when(noteService.getNotesByTag(testUsername, "test")).thenReturn(Arrays.asList(testNoteDto));

        // When & Then
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
        // Given
        when(noteService.getNotesByCartella(testUsername, "Test Folder")).thenReturn(Arrays.asList(testNoteDto));

        // When & Then
        mockMvc.perform(get("/api/notes/filter/cartella/Test Folder")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.cartella").value("Test Folder"));

        verify(noteService).getNotesByCartella(testUsername, "Test Folder");
    }
}