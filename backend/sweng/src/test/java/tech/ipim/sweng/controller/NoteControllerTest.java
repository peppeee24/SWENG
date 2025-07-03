package tech.ipim.sweng.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.ipim.sweng.config.TestConfig;
import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.LockStatusDto;
import tech.ipim.sweng.dto.NoteDto;
import tech.ipim.sweng.dto.PermissionDto;
import tech.ipim.sweng.dto.UpdateNoteRequest;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.service.NoteLockService;
import tech.ipim.sweng.service.NoteService;
import tech.ipim.sweng.util.JwtUtil;

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
    private NoteLockService noteLockService;

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

        // Mock per il token standard
        when(jwtUtil.extractTokenFromHeader(validToken)).thenReturn("valid.jwt.token");
        when(jwtUtil.isTokenValid("valid.jwt.token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid.jwt.token")).thenReturn(testUsername);

        // Mock per token usati nei test di update
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
        NoteService.UserStatsDto stats = new NoteService.UserStatsDto(
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

        LockStatusDto lockStatus = new LockStatusDto(false, null, null, true);
        when(noteLockService.getLockStatus(1L, "testuser")).thenReturn(lockStatus);
        
        when(noteLockService.tryLockNote(1L, "testuser")).thenReturn(true);

        when(noteService.updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser")))
                .thenReturn(updatedNote);

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
        verify(noteLockService).unlockNote(1L, "testuser");
    }

    @Test
    @DisplayName("PUT /api/notes/{id} - Dovrebbe fallire con token non valido")
    void shouldFailUpdateWithInvalidToken() throws Exception {
        String invalidToken = "Bearer invalid-token";
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Test");
        request.setContenuto("Test content");

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
        String token = "Bearer valid-jwt-token";
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("");
        request.setContenuto("Contenuto valido");

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
        String token = "Bearer valid-jwt-token";
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Tentativo Modifica");
        request.setContenuto("Non autorizzato");

        LockStatusDto lockStatus = new LockStatusDto(false, null, null, false);
        when(noteLockService.getLockStatus(1L, "testuser")).thenReturn(lockStatus);
        
        when(noteLockService.tryLockNote(1L, "testuser")).thenReturn(false);

        mockMvc.perform(put("/api/notes/1")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Impossibile acquisire il lock sulla nota"));

        verify(noteService, never()).updateNote(anyLong(), any(UpdateNoteRequest.class), anyString());
    }

    @Test
    @DisplayName("PUT /api/notes/{id} - Dovrebbe fallire quando la nota è bloccata da altro utente")
    void shouldFailUpdateWhenNoteIsLockedByOther() throws Exception {
        String token = "Bearer valid-jwt-token";
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Test");
        request.setContenuto("Test content");

        LockStatusDto lockStatus = new LockStatusDto(true, "other-user", LocalDateTime.now().plusMinutes(5), false);
        when(noteLockService.getLockStatus(1L, "testuser")).thenReturn(lockStatus);

        mockMvc.perform(put("/api/notes/1")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("La nota è in modifica da other-user"));

        verify(noteService, never()).updateNote(anyLong(), any(UpdateNoteRequest.class), anyString());
    }

    @Test
    @DisplayName("PUT /api/notes/{id}/permissions - Dovrebbe aggiornare i permessi con successo")
    @WithMockUser(username = "testuser")
    void shouldUpdatePermissionsSuccessfully() throws Exception {
        PermissionDto permissionDto = new PermissionDto();
        permissionDto.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA);
        permissionDto.setUtentiLettura(Arrays.asList("user1", "user2"));
        permissionDto.setUtentiScrittura(Arrays.asList());

        NoteDto updatedNote = new NoteDto();
        updatedNote.setId(1L);
        updatedNote.setTitolo("Test Note");
        updatedNote.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA.name());
        updatedNote.setPermessiLettura(Set.of("user1", "user2"));
        updatedNote.setPermessiScrittura(Set.of());

        when(noteService.updateNotePermissions(eq(1L), any(PermissionDto.class), eq(testUsername)))
                .thenReturn(updatedNote);

        mockMvc.perform(put("/api/notes/1/permissions")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permissionDto))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Permessi aggiornati con successo"))
                .andExpect(jsonPath("$.note.id").value(1))
                .andExpect(jsonPath("$.note.tipoPermesso").value("CONDIVISA_LETTURA"));

        verify(noteService).updateNotePermissions(eq(1L), any(PermissionDto.class), eq(testUsername));
    }

    @Test
    @DisplayName("PUT /api/notes/{id}/permissions - Dovrebbe fallire se non è il proprietario")
    @WithMockUser(username = "testuser")
    void shouldFailPermissionsUpdateWhenNotOwner() throws Exception {
        PermissionDto permissionDto = new PermissionDto();
        permissionDto.setTipoPermesso(TipoPermesso.PRIVATA);

        when(noteService.updateNotePermissions(eq(1L), any(PermissionDto.class), eq(testUsername)))
                .thenThrow(new RuntimeException("Solo il proprietario può modificare i permessi di questa nota"));

        mockMvc.perform(put("/api/notes/1/permissions")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permissionDto))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Solo il proprietario può modificare i permessi di questa nota"));

        verify(noteService).updateNotePermissions(eq(1L), any(PermissionDto.class), eq(testUsername));
    }

    @Test
    @DisplayName("TTD-CONTROLLER-LOCK-001: Test NoteLockService è iniettato nel controller")
    void testNoteLockServiceInjected() {
        assertNotNull(noteLockService);
    }

    @Test
    @DisplayName("TTD-CONTROLLER-LOCK-002: Test mock tryLockNote funziona")
    void testTryLockNoteMock() {
        when(noteLockService.tryLockNote(anyLong(), anyString())).thenReturn(true);
        
        boolean result = noteLockService.tryLockNote(1L, "testuser");
        
        assertTrue(result);
        verify(noteLockService).tryLockNote(1L, "testuser");
    }

    @Test
    @DisplayName("TTD-CONTROLLER-LOCK-003: Test mock unlockNote funziona")
    void testUnlockNoteMock() {
        assertDoesNotThrow(() -> {
            noteLockService.unlockNote(1L, "testuser");
        });
        
        verify(noteLockService).unlockNote(1L, "testuser");
    }

    @Test
    @DisplayName("TTD-CONTROLLER-LOCK-004: Test mock getLockStatus funziona")
    void testGetLockStatusMock() {

        LockStatusDto mockStatus = new LockStatusDto(false, null, null, true);
        when(noteLockService.getLockStatus(anyLong(), anyString())).thenReturn(mockStatus);
        
        LockStatusDto result = noteLockService.getLockStatus(1L, "testuser");
        
        assertNotNull(result);
        assertFalse(result.isLocked());
        assertTrue(result.canEdit());
        
        verify(noteLockService).getLockStatus(1L, "testuser");
    }

    @Test
    @DisplayName("TTD-CONTROLLER-LOCK-005: Test mock refreshLock funziona")
    void testRefreshLockMock() {
        assertDoesNotThrow(() -> {
            noteLockService.refreshLock(1L, "testuser");
        });
        
        verify(noteLockService).refreshLock(1L, "testuser");
    }

    private void assertDoesNotThrow(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            throw new AssertionError("Expected no exception but got: " + e.getMessage(), e);
        }
    }
}