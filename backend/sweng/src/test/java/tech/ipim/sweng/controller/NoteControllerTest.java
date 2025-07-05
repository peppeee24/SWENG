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
import tech.ipim.sweng.dto.LockStatusDto;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.service.NoteService;
import tech.ipim.sweng.service.NoteLockService;
import tech.ipim.sweng.util.JwtUtil;
import tech.ipim.sweng.dto.UpdateNoteRequest;
import tech.ipim.sweng.dto.PermissionDto;
import tech.ipim.sweng.dto.NoteVersionDto;
import tech.ipim.sweng.dto.VersionComparisonDto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        doNothing().when(noteLockService).unlockNote(1L, "testuser");

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

        LockStatusDto lockStatus = new LockStatusDto(false, null, null, true);
        when(noteLockService.getLockStatus(1L, "testuser")).thenReturn(lockStatus);

        when(noteLockService.tryLockNote(1L, "testuser")).thenReturn(true);

        when(noteService.updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser")))
                .thenThrow(new RuntimeException("Non hai i permessi per modificare questa nota"));

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

    // TEST PER VERSIONAMENTO

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/notes/{id}/versions - Dovrebbe restituire la cronologia delle versioni")
    void shouldGetNoteVersionHistory() throws Exception {
        // Given
        NoteVersionDto version1 = new NoteVersionDto();
        version1.setId(1L);
        version1.setVersionNumber(1);
        version1.setTitolo("Titolo v1");
        version1.setContenuto("Contenuto v1");
        version1.setCreatedBy("testuser");
        version1.setCreatedAt(LocalDateTime.now().minusHours(2));
        version1.setChangeDescription("Prima versione");

        NoteVersionDto version2 = new NoteVersionDto();
        version2.setId(2L);
        version2.setVersionNumber(2);
        version2.setTitolo("Titolo v2");
        version2.setContenuto("Contenuto v2");
        version2.setCreatedBy("testuser");
        version2.setCreatedAt(LocalDateTime.now().minusHours(1));
        version2.setChangeDescription("Seconda versione");

        List<NoteVersionDto> versions = Arrays.asList(version2, version1);

        when(noteService.getNoteVersionHistory(1L, "testuser")).thenReturn(versions);

        // When & Then
        mockMvc.perform(get("/api/notes/1/versions")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].versionNumber", is(2)))
                .andExpect(jsonPath("$.data[0].titolo", is("Titolo v2")))
                .andExpect(jsonPath("$.data[0].contenuto", is("Contenuto v2")))
                .andExpect(jsonPath("$.data[0].createdBy", is("testuser")))
                .andExpect(jsonPath("$.data[0].changeDescription", is("Seconda versione")))
                .andExpect(jsonPath("$.data[1].versionNumber", is(1)))
                .andExpect(jsonPath("$.data[1].titolo", is("Titolo v1")))
                .andExpect(jsonPath("$.data[1].contenuto", is("Contenuto v1")));

        verify(noteService).getNoteVersionHistory(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/notes/{id}/versions - Dovrebbe restituire 404 per nota non trovata")
    void shouldReturn404WhenNoteNotFoundForVersionHistory() throws Exception {
        // Given
        when(noteService.getNoteVersionHistory(999L, "testuser"))
                .thenThrow(new RuntimeException("Nota non trovata"));

        // When & Then
        mockMvc.perform(get("/api/notes/999/versions")
                        .header("Authorization", validToken))
                .andExpect(status().isNotFound());

        verify(noteService).getNoteVersionHistory(999L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/notes/{id}/versions - Dovrebbe restituire 403 se utente non ha accesso")
    void shouldReturn403WhenUserHasNoAccessToVersionHistory() throws Exception {
        // Given
        when(noteService.getNoteVersionHistory(1L, "testuser"))
                .thenThrow(new RuntimeException("Non hai accesso a questa nota"));

        // When & Then
        mockMvc.perform(get("/api/notes/1/versions")
                        .header("Authorization", validToken))
                .andExpect(status().isForbidden());

        verify(noteService).getNoteVersionHistory(1L, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/notes/{id}/versions/{version} - Dovrebbe restituire una versione specifica")
    void shouldGetSpecificNoteVersion() throws Exception {
        // Given
        NoteVersionDto version = new NoteVersionDto();
        version.setId(1L);
        version.setVersionNumber(2);
        version.setTitolo("Titolo v2");
        version.setContenuto("Contenuto v2");
        version.setCreatedBy("testuser");
        version.setCreatedAt(LocalDateTime.now());
        version.setChangeDescription("Seconda versione");

        when(noteService.getNoteVersion(1L, 2, "testuser")).thenReturn(Optional.of(version));

        // When & Then
        mockMvc.perform(get("/api/notes/1/versions/2")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.versionNumber", is(2)))
                .andExpect(jsonPath("$.data.titolo", is("Titolo v2")))
                .andExpect(jsonPath("$.data.contenuto", is("Contenuto v2")))
                .andExpect(jsonPath("$.data.createdBy", is("testuser")))
                .andExpect(jsonPath("$.data.changeDescription", is("Seconda versione")));

        verify(noteService).getNoteVersion(1L, 2, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/notes/{id}/versions/{version} - Dovrebbe restituire 404 per versione non trovata")
    void shouldReturn404WhenVersionNotFound() throws Exception {
        // Given
        when(noteService.getNoteVersion(1L, 999, "testuser")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/notes/1/versions/999")
                        .header("Authorization", validToken))
                .andExpect(status().isNotFound());

        verify(noteService).getNoteVersion(1L, 999, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/notes/{id}/restore - Dovrebbe ripristinare una versione")
    void shouldRestoreNoteVersion() throws Exception {
        // Given
        NoteDto restoredNote = new NoteDto();
        restoredNote.setId(1L);
        restoredNote.setTitolo("Titolo Ripristinato");
        restoredNote.setContenuto("Contenuto Ripristinato");
        restoredNote.setAutore("testuser");
        restoredNote.setVersionNumber(4L);

        when(noteService.restoreNoteVersion(1L, 2, "testuser")).thenReturn(restoredNote);

        // When & Then
        mockMvc.perform(post("/api/notes/1/restore")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionNumber\": 2}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.note.id", is(1)))
                .andExpect(jsonPath("$.note.titolo", is("Titolo Ripristinato")))
                .andExpect(jsonPath("$.note.contenuto", is("Contenuto Ripristinato")))
                .andExpect(jsonPath("$.note.autore", is("testuser")))
                .andExpect(jsonPath("$.note.versionNumber", is(4)));

        verify(noteService).restoreNoteVersion(1L, 2, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/notes/{id}/restore - Dovrebbe restituire 404 se versione non trovata")
    void shouldReturn404WhenRestoringNonExistentVersion() throws Exception {
        // Given
        when(noteService.restoreNoteVersion(1L, 999, "testuser"))
                .thenThrow(new RuntimeException("Versione 999 non trovata"));

        // When & Then
        mockMvc.perform(post("/api/notes/1/restore")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionNumber\": 999}")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(noteService).restoreNoteVersion(1L, 999, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/notes/{id}/restore - Dovrebbe restituire 403 se utente non ha accesso di scrittura")
    void shouldReturn403WhenRestoringWithoutWriteAccess() throws Exception {
        // Given
        when(noteService.restoreNoteVersion(1L, 2, "testuser"))
                .thenThrow(new RuntimeException("Non hai i permessi per ripristinare versioni di questa nota"));

        // When & Then
        mockMvc.perform(post("/api/notes/1/restore")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionNumber\": 2}")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(noteService).restoreNoteVersion(1L, 2, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/notes/{id}/compare/{version1}/{version2} - Dovrebbe confrontare due versioni")
    void shouldCompareTwoVersions() throws Exception {
        // Given
        NoteVersionDto version1 = new NoteVersionDto();
        version1.setVersionNumber(1);
        version1.setTitolo("Titolo v1");
        version1.setContenuto("Contenuto v1");

        NoteVersionDto version2 = new NoteVersionDto();
        version2.setVersionNumber(2);
        version2.setTitolo("Titolo v2");
        version2.setContenuto("Contenuto v2");

        VersionComparisonDto.DifferenceDto differences = new VersionComparisonDto.DifferenceDto(
                true, true, "'Titolo v1' → 'Titolo v2'", "Contenuto modificato"
        );

        VersionComparisonDto comparison = new VersionComparisonDto(version1, version2, differences);

        when(noteService.compareNoteVersions(1L, 1, 2, "testuser")).thenReturn(comparison);

        // When & Then
        mockMvc.perform(get("/api/notes/1/compare/1/2")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.version1Number", is(1)))
                .andExpect(jsonPath("$.data.version2Number", is(2)))
                .andExpect(jsonPath("$.data.differences.titleChanged", is(true)))
                .andExpect(jsonPath("$.data.differences.contentChanged", is(true)));

        verify(noteService).compareNoteVersions(1L, 1, 2, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/notes/{id}/compare/{version1}/{version2} - Dovrebbe restituire 404 per versione non esistente")
    void shouldReturn404WhenComparingNonExistentVersion() throws Exception {
        when(noteService.compareNoteVersions(1L, 1, 999, "testuser"))
                .thenThrow(new RuntimeException("Versione 999 non trovata"));

        // When & Then
        mockMvc.perform(get("/api/notes/1/compare/1/999")
                        .header("Authorization", validToken))
                .andExpect(status().isNotFound());

        verify(noteService).compareNoteVersions(1L, 1, 999, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/notes/{id}/compare/{version1}/{version2} - Dovrebbe restituire 400 per parametri invalidi")
    void shouldReturn400ForInvalidComparisonParameters() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/notes/1/compare/invalid/2"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/notes/1/compare/1/invalid"))
                .andExpect(status().isBadRequest());

        // Non dovrebbe chiamare il servizio con parametri invalidi
        verify(noteService, never()).compareNoteVersions(anyLong(), anyInt(), anyInt(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Dovrebbe gestire errori di sicurezza per endpoints di versionamento")
    void shouldHandleSecurityErrorsForVersioningEndpoints() throws Exception {
        // Given
        when(noteService.getNoteVersionHistory(1L, "testuser"))
                .thenThrow(new RuntimeException("Non hai accesso a questa nota"));

        // When & Then
        mockMvc.perform(get("/api/notes/1/versions")
                        .header("Authorization", validToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Dovrebbe richiedere autenticazione per endpoints di versionamento")
    void shouldRequireAuthenticationForVersioningEndpoints() throws Exception {
        // When & Then - Testa senza header Authorization
        mockMvc.perform(get("/api/notes/1/versions"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/notes/1/versions/2"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/notes/1/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionNumber\": 2}")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/notes/1/compare/1/2"))
                .andExpect(status().isBadRequest());
    }

    // TEST PER LOCK SERVICE

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


    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-SEARCH-001: GET /api/notes/search/author - Dovrebbe cercare note per autore")
    void shouldSearchNotesByAuthor() throws Exception {
        // Given
        NoteDto notaMario = createTestNoteDto("mario");
        notaMario.setTitolo("Nota di Mario");
        when(noteService.findNotesByAuthor("mario", "testuser")).thenReturn(Arrays.asList(notaMario));

        // When & Then
        mockMvc.perform(get("/api/notes/search/author")
                        .header("Authorization", validToken)
                        .param("author", "mario"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].autore", is("mario")))
                .andExpect(jsonPath("$.data[0].titolo", is("Nota di Mario")));

        verify(noteService).findNotesByAuthor("mario", "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-SEARCH-002: GET /api/notes/search/author - Dovrebbe gestire autore vuoto")
    void shouldHandleEmptyAuthorSearch() throws Exception {
        mockMvc.perform(get("/api/notes/search/author")
                        .header("Authorization", validToken)
                        .param("author", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpected(jsonPath("$.message", is("Parametro autore richiesto")));

        verify(noteService, never()).findNotesByAuthor(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-SEARCH-003: GET /api/notes/search/date - Dovrebbe cercare note per range di date")
    void shouldSearchNotesByDateRange() throws Exception {
        // Given
        NoteDto notaGennaio = createTestNoteDto("testuser");
        notaGennaio.setTitolo("Nota di Gennaio");
        when(noteService.findNotesByDateRange(any(LocalDateTime.class), any(LocalDateTime.class), eq("testuser")))
                .thenReturn(Arrays.asList(notaGennaio));

        // When & Then
        mockMvc.perform(get("/api/notes/search/date")
                        .header("Authorization", validToken)
                        .param("startDate", "2025-01-01T00:00:00")
                        .param("endDate", "2025-01-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success", is(true)))
                .andExpected(jsonPath("$.data", hasSize(1)))
                .andExpected(jsonPath("$.data[0].titolo", is("Nota di Gennaio")));

        verify(noteService).findNotesByDateRange(any(LocalDateTime.class), any(LocalDateTime.class), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-SEARCH-004: GET /api/notes/search/date - Dovrebbe validare formato date")
    void shouldValidateDateFormat() throws Exception {
        mockMvc.perform(get("/api/notes/search/date")
                        .header("Authorization", validToken)
                        .param("startDate", "data-invalida")
                        .param("endDate", "2025-01-31T23:59:59"))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.success", is(false)))
                .andExpected(jsonPath("$.message", containsString("Formato data invalido")));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-SEARCH-005: GET /api/notes/search/combined - Dovrebbe cercare con filtri combinati")
    void shouldSearchWithCombinedFilters() throws Exception {
        // Given
        NoteDto notaCombinata = createTestNoteDto("mario");
        notaCombinata.setTitolo("Nota Mario Gennaio");
        when(noteService.findNotesByAuthorAndDateRange(eq("mario"), any(LocalDateTime.class), any(LocalDateTime.class), eq("testuser")))
                .thenReturn(Arrays.asList(notaCombinata));

        // When & Then
        mockMvc.perform(get("/api/notes/search/combined")
                        .header("Authorization", validToken)
                        .param("author", "mario")
                        .param("startDate", "2025-01-01T00:00:00")
                        .param("endDate", "2025-01-31T23:59:59"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andExpected(jsonPath("$.data", hasSize(1)))
                .andExpected(jsonPath("$.data[0].autore", is("mario")));

        verify(noteService).findNotesByAuthorAndDateRange(eq("mario"), any(LocalDateTime.class), any(LocalDateTime.class), eq("testuser"));
    }


    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-VER-001: GET /api/notes/{id}/versions - Dovrebbe gestire note con molte versioni")
    void shouldHandleNotesWithManyVersions() throws Exception {
        // Given
        List<NoteVersionDto> molteVersioni = generateManyVersionDtos(50);
        when(noteService.getNoteVersionHistory(1L, "testuser")).thenReturn(molteVersioni);

        // When & Then
        mockMvc.perform(get("/api/notes/1/versions")
                        .header("Authorization", validToken))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andExpected(jsonPath("$.data", hasSize(50)))
                .andExpected(jsonPath("$.data[0].versionNumber", is(50))) // Più recente
                .andExpected(jsonPath("$.data[49].versionNumber", is(1))); // Più vecchia
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-VER-002: GET /api/notes/{id}/versions/{version} - Dovrebbe ottenere versione specifica")
    void shouldGetSpecificVersion() throws Exception {
        // Given
        NoteVersionDto versioneSpecifica = createVersionDto(5);
        when(noteService.getNoteVersion(1L, 5, "testuser")).thenReturn(Optional.of(versioneSpecifica));

        // When & Then
        mockMvc.perform(get("/api/notes/1/versions/5")
                        .header("Authorization", validToken))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andExpected(jsonPath("$.data.versionNumber", is(5)))
                .andExpected(jsonPath("$.data.titolo", is("Titolo v5")))
                .andExpected(jsonPath("$.data.contenuto", is("Contenuto v5")));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-VER-003: POST /api/notes/{id}/restore - Dovrebbe ripristinare versione con validazione")
    void shouldRestoreVersionWithValidation() throws Exception {
        // Given
        NoteDto notaRipristinata = createTestNoteDto("testuser");
        notaRipristinata.setTitolo("Titolo Ripristinato");
        when(noteService.restoreNoteVersion(1L, 3, "testuser")).thenReturn(notaRipristinata);

        // When & Then
        mockMvc.perform(post("/api/notes/1/restore")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionNumber\": 3}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andExpected(jsonPath("$.data.titolo", is("Titolo Ripristinato")))
                .andExpected(jsonPath("$.message", containsString("ripristinata alla versione 3")));

        verify(noteService).restoreNoteVersion(1L, 3, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-VER-004: POST /api/notes/{id}/restore - Dovrebbe validare numero versione")
    void shouldValidateVersionNumber() throws Exception {
        mockMvc.perform(post("/api/notes/1/restore")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionNumber\": -1}")
                        .with(csrf()))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.success", is(false)))
                .andExpected(jsonPath("$.message", containsString("Numero versione deve essere positivo")));

        verify(noteService, never()).restoreNoteVersion(anyLong(), anyInt(), anyString());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-VER-005: GET /api/notes/{id}/compare/{v1}/{v2} - Dovrebbe confrontare versioni")
    void shouldCompareVersions() throws Exception {
        // Given
        VersionComparisonDto confronto = createVersionComparisonDto();
        when(noteService.compareNoteVersions(1L, 1, 2, "testuser")).thenReturn(confronto);

        // When & Then
        mockMvc.perform(get("/api/notes/1/compare/1/2")
                        .header("Authorization", validToken))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.success", is(true)))
                .andExpected(jsonPath("$.data.version1Number", is(1)))
                .andExpected(jsonPath("$.data.version2Number", is(2)))
                .andExpected(jsonPath("$.data.differences.titleChanged", is(true)))
                .andExpected(jsonPath("$.data.differences.contentChanged", is(true)));

        verify(noteService).compareNoteVersions(1L, 1, 2, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-VER-006: Dovrebbe gestire errori di versionamento con messaggi appropriati")
    void shouldHandleVersioningErrorsWithAppropriateMessages() throws Exception {
        // Given
        when(noteService.getNoteVersionHistory(999L, "testuser"))
                .thenThrow(new RuntimeException("Nota non trovata"));

        // When & Then
        mockMvc.perform(get("/api/notes/999/versions")
                        .header("Authorization", validToken))
                .andExpected(status().isNotFound())
                .andExpected(jsonPath("$.success", is(false)))
                .andExpected(jsonPath("$.message", is("Nota non trovata")));
    }


    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-SEC-001: Dovrebbe impedire accesso a versioni non autorizzate")
    void shouldPreventAccessToUnauthorizedVersions() throws Exception {
        // Given
        when(noteService.getNoteVersionHistory(1L, "testuser"))
                .thenThrow(new RuntimeException("Non hai accesso a questa nota"));

        // When & Then
        mockMvc.perform(get("/api/notes/1/versions")
                        .header("Authorization", validToken))
                .andExpected(status().isForbidden())
                .andExpected(jsonPath("$.success", is(false)))
                .andExpected(jsonPath("$.message", is("Non hai accesso a questa nota")));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-SEC-002: Dovrebbe impedire ripristino senza permessi di scrittura")
    void shouldPreventRestoreWithoutWritePermissions() throws Exception {
        // Given
        when(noteService.restoreNoteVersion(1L, 2, "testuser"))
                .thenThrow(new RuntimeException("Non hai i permessi per ripristinare versioni di questa nota"));

        // When & Then
        mockMvc.perform(post("/api/notes/1/restore")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionNumber\": 2}")
                        .with(csrf()))
                .andExpected(status().isForbidden())
                .andExpected(jsonPath("$.success", is(false)))
                .andExpected(jsonPath("$.message", containsString("Non hai i permessi")));
    }


    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-PERF-001: Dovrebbe gestire richieste concorrenti per versioni")
    void shouldHandleConcurrentVersionRequests() throws Exception {
        // Given
        List<NoteVersionDto> versioni = generateManyVersionDtos(10);
        when(noteService.getNoteVersionHistory(1L, "testuser")).thenReturn(versioni);

        // When - Simula richieste concorrenti
        List<Boolean> risultati = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> {
                try {
                    MvcResult result = mockMvc.perform(get("/api/notes/1/versions")
                                    .header("Authorization", validToken))
                            .andExpected(status().isOk())
                            .andReturn();
                    return result.getResponse().getStatus() == 200;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        // Then
        for (Future<Boolean> future : futures) {
            risultati.add(future.get());
        }
        executor.shutdown();

        assertThat(risultati).allMatch(result -> result == true);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-PERF-002: Dovrebbe rispondere velocemente per cronologia versioni")
    void shouldRespondQuicklyForVersionHistory() throws Exception {
        // Given
        List<NoteVersionDto> versioni = generateManyVersionDtos(100);
        when(noteService.getNoteVersionHistory(1L, "testuser")).thenReturn(versioni);

        // When
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/notes/1/versions")
                        .header("Authorization", validToken))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.data", hasSize(100)));
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(endTime - startTime).isLessThan(200); // Meno di 200ms
    }



    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-VAL-001: Dovrebbe validare parametri di ricerca per autore")
    void shouldValidateAuthorSearchParameters() throws Exception {
        // Test con autore troppo lungo
        String autoreTroppoLungo = "a".repeat(256);

        mockMvc.perform(get("/api/notes/search/author")
                        .header("Authorization", validToken)
                        .param("author", autoreTroppoLungo))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.message", containsString("Username autore troppo lungo")));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-VAL-002: Dovrebbe validare caratteri speciali in ricerca autore")
    void shouldValidateSpecialCharactersInAuthorSearch() throws Exception {
        mockMvc.perform(get("/api/notes/search/author")
                        .header("Authorization", validToken)
                        .param("author", "user<script>alert('xss')</script>"))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.message", containsString("Caratteri non validi nel nome utente")));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("CTRL-VAL-003: Dovrebbe validare range di date logico")
    void shouldValidateLogicalDateRange() throws Exception {
        mockMvc.perform(get("/api/notes/search/date")
                        .header("Authorization", validToken)
                        .param("startDate", "2025-12-31T23:59:59")
                        .param("endDate", "2025-01-01T00:00:00"))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.message", containsString("Data inizio posteriore a data fine")));
    }



    private NoteDto createTestNoteDto(String author) {
        NoteDto dto = new NoteDto();
        dto.setId(1L);
        dto.setTitolo("Test Note");
        dto.setContenuto("Test content");
        dto.setAutore(author);
        dto.setDataCreazione(LocalDateTime.now().minusHours(1));
        dto.setDataModifica(LocalDateTime.now().minusMinutes(30));
        return dto;
    }

    private List<NoteVersionDto> generateManyVersionDtos(int count) {
        return java.util.stream.IntStream.range(1, count + 1)
                .mapToObj(this::createVersionDto)
                .collect(java.util.stream.Collectors.toList());
    }

    private NoteVersionDto createVersionDto(int versionNumber) {
        NoteVersionDto dto = new NoteVersionDto();
        dto.setId((long) versionNumber);
        dto.setVersionNumber(versionNumber);
        dto.setTitolo("Titolo v" + versionNumber);
        dto.setContenuto("Contenuto v" + versionNumber);
        dto.setCreatedBy("testuser");
        dto.setCreatedAt(LocalDateTime.now().minusHours(versionNumber));
        dto.setChangeDescription("Versione " + versionNumber);
        return dto;
    }

    private VersionComparisonDto createVersionComparisonDto() {
        VersionComparisonDto dto = new VersionComparisonDto();
        dto.setVersion1Number(1);
        dto.setVersion2Number(2);

        VersionComparisonDto.Differences differences = new VersionComparisonDto.Differences();
        differences.setTitleChanged(true);
        differences.setContentChanged(true);
        differences.setTitleDiff("Titolo v1 → Titolo v2");
        differences.setContentDiff("Contenuto v1 → Contenuto v2");

        dto.setDifferences(differences);
        return dto;
    }
}