package tech.ipim.sweng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tech.ipim.sweng.dto.NoteDto;
import tech.ipim.sweng.service.NoteService;
import tech.ipim.sweng.service.UserService;
import tech.ipim.sweng.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoteController.class)
class NoteControllerSearchTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoteService noteService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private NoteDto testNoteDto;
    private List<NoteDto> testNotes;
    private String validToken;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testUsername = "testuser";
        validToken = "Bearer valid-jwt-token";

        testNoteDto = new NoteDto();
        testNoteDto.setId(1L);
        testNoteDto.setTitolo("Test Note");
        testNoteDto.setContenuto("Test content");
        testNoteDto.setAutore("testuser");
        testNoteDto.setCartelle(Set.of("Lavoro"));
        testNoteDto.setDataCreazione(LocalDateTime.of(2024, 1, 15, 10, 0));
        testNoteDto.setDataModifica(LocalDateTime.of(2024, 1, 16, 12, 0));

        testNotes = Arrays.asList(testNoteDto);

        // Mock JWT
        when(jwtUtil.extractTokenFromHeader(validToken)).thenReturn("valid-jwt-token");
        when(jwtUtil.isTokenValid("valid-jwt-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-jwt-token")).thenReturn(testUsername);
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldSearchNotesByKeyword() throws Exception {
        // Given
        when(noteService.searchNotes("testuser", "test")).thenReturn(testNotes);

        // When & Then
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", validToken)
                .param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes").isArray())
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].id").value(1))
                .andExpect(jsonPath("$.notes[0].titolo").value("Test Note"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.keyword").value("test"));

        verify(noteService).searchNotes("testuser", "test");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnEmptyResultWhenNoNotesFoundByKeyword() throws Exception {
        // Given
        when(noteService.searchNotes("testuser", "inesistente")).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", validToken)
                .param("q", "inesistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes").isArray())
                .andExpect(jsonPath("$.notes").isEmpty())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.keyword").value("inesistente"));

        verify(noteService).searchNotes("testuser", "inesistente");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetNotesByTag() throws Exception {
        // Given
        when(noteService.getNotesByTag("testuser", "importante")).thenReturn(testNotes);

        // When & Then
        mockMvc.perform(get("/api/notes/filter/tag/importante")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes").isArray())
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.tag").value("importante"));

        verify(noteService).getNotesByTag("testuser", "importante");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetNotesByCartella() throws Exception {
        // Given
        when(noteService.getNotesByCartella("testuser", "Lavoro")).thenReturn(testNotes);

        // When & Then
        mockMvc.perform(get("/api/notes/filter/cartella/Lavoro")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes").isArray())
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.cartella").value("Lavoro"));

        verify(noteService).getNotesByCartella("testuser", "Lavoro");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetAllNotesWithFilter() throws Exception {
        // Given
        when(noteService.getAllAccessibleNotes("testuser")).thenReturn(testNotes);

        // When & Then
        mockMvc.perform(get("/api/notes")
                .header("Authorization", validToken)
                .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes").isArray())
                .andExpect(jsonPath("$.notes", hasSize(1)));

        verify(noteService).getAllAccessibleNotes("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetOwnNotesWithFilter() throws Exception {
        // Given
        when(noteService.getUserNotes("testuser")).thenReturn(testNotes);

        // When & Then
        mockMvc.perform(get("/api/notes")
                .header("Authorization", validToken)
                .param("filter", "own"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes").isArray())
                .andExpect(jsonPath("$.notes", hasSize(1)));

        verify(noteService).getUserNotes("testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetNotesWithSearchParam() throws Exception {
        // Given
        when(noteService.searchNotes("testuser", "java")).thenReturn(testNotes);

        // When & Then
        mockMvc.perform(get("/api/notes")
                .header("Authorization", validToken)
                .param("search", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)));

        verify(noteService).searchNotes("testuser", "java");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetNotesWithTagParam() throws Exception {
        // Given
        when(noteService.getNotesByTag("testuser", "importante")).thenReturn(testNotes);

        // When & Then
        mockMvc.perform(get("/api/notes")
                .header("Authorization", validToken)
                .param("tag", "importante"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)));

        verify(noteService).getNotesByTag("testuser", "importante");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetNotesWithCartellaParam() throws Exception {
        // Given
        when(noteService.getNotesByCartella("testuser", "Lavoro")).thenReturn(testNotes);

        // When & Then
        mockMvc.perform(get("/api/notes")
                .header("Authorization", validToken)
                .param("cartella", "Lavoro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)));

        verify(noteService).getNotesByCartella("testuser", "Lavoro");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetNotesWithAutoreParam() throws Exception {
        // Given
        when(noteService.getNotesByAutore("testuser", "otheruser", "all")).thenReturn(testNotes);

        // When & Then
        mockMvc.perform(get("/api/notes")
                .header("Authorization", validToken)
                .param("autore", "otheruser")
                .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)));

        verify(noteService).getNotesByAutore("testuser", "otheruser", "all");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetNotesWithDateRangeParams() throws Exception {
        // Given
        when(noteService.getNotesByDateRange("testuser", "2024-01-01", "2024-01-31", "all"))
                .thenReturn(testNotes);

        // When & Then
        mockMvc.perform(get("/api/notes")
                .header("Authorization", validToken)
                .param("dataInizio", "2024-01-01")
                .param("dataFine", "2024-01-31")
                .param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes", hasSize(1)));

        verify(noteService).getNotesByDateRange("testuser", "2024-01-01", "2024-01-31", "all");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldGetNoteById() throws Exception {
        // Given
        when(noteService.getNoteById(1L, "testuser")).thenReturn(Optional.of(testNoteDto));

        // When & Then
        mockMvc.perform(get("/api/notes/1")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.note.id").value(1))
                .andExpect(jsonPath("$.note.titolo").value("Test Note"));

        verify(noteService).getNoteById(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnNotFoundWhenNoteDoesNotExist() throws Exception {
        // Given
        when(noteService.getNoteById(999L, "testuser")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/notes/999")
                .header("Authorization", validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Nota non trovata o non accessibile"));

        verify(noteService).getNoteById(999L, "testuser");
    }

    @Test
    void shouldRequireAuthenticationForSearchEndpoints() throws Exception {
        // Test ricerca per keyword
        mockMvc.perform(get("/api/notes/search")
                .param("q", "test"))
                .andExpect(status().isUnauthorized());

        // Test ricerca per tag
        mockMvc.perform(get("/api/notes/filter/tag/importante"))
                .andExpect(status().isUnauthorized());

        // Test ricerca per cartella
        mockMvc.perform(get("/api/notes/filter/cartella/Lavoro"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireAuthenticationsForSearchEndpoints() throws Exception {
        // Test ricerca per keyword
        mockMvc.perform(get("/api/notes/search")
                .param("q", "test"))
                .andExpect(status().isUnauthorized());

        // Test ricerca per tag
        mockMvc.perform(get("/api/notes/filter/tag/importante"))
                .andExpect(status().isUnauthorized());

        // Test ricerca per cartella
        mockMvc.perform(get("/api/notes/filter/cartella/Lavoro"))
                .andExpect(status().isUnauthorized());

        // Test get all notes
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleServiceExceptionGracefully() throws Exception {
        // Given
        when(noteService.searchNotes("testuser", "error")).thenThrow(new RuntimeException("Errore interno"));

        // When & Then
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", validToken)
                .param("q", "error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Errore durante la ricerca"));

        verify(noteService).searchNotes("testuser", "error");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleEmptySearchKeyword() throws Exception {
        // Given
        when(noteService.searchNotes("testuser", "")).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", validToken)
                .param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.notes").isEmpty())
                .andExpect(jsonPath("$.count").value(0));

        verify(noteService).searchNotes("testuser", "");
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldHandleSpecialCharactersInSearch() throws Exception {
        // Given
        String specialKeyword = "test@#$%";
        when(noteService.searchNotes("testuser", specialKeyword)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/notes/search")
                .header("Authorization", validToken)
                .param("q", specialKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.keyword").value(specialKeyword));

        verify(noteService).searchNotes("testuser", specialKeyword);
    }
} 