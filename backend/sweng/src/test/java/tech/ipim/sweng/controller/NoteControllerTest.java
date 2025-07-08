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
import static org.hamcrest.Matchers.containsString;




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


        // Verificare -----------
        when(jwtUtil.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn(testUsername);
        when(noteLockService.getNoteLockOwner(anyLong())).thenReturn("other-user");
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
    @DisplayName("UC4.C16 - Test endpoint PUT /notes/{id} con permessi scrittura")
    @WithMockUser(username = "testuser")
    void testUpdateNoteWithWritePermissions() throws Exception {
        // Arrange
        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitolo("Titolo Aggiornato");
        updateRequest.setContenuto("Contenuto aggiornato");
        updateRequest.setTags(Set.of("updated"));

        NoteDto updatedNote = new NoteDto();
        updatedNote.setId(1L);
        updatedNote.setTitolo("Titolo Aggiornato");
        updatedNote.setContenuto("Contenuto aggiornato");
        updatedNote.setTags(Set.of("updated"));

        when(jwtUtil.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");


        LockStatusDto lockStatus = new LockStatusDto(false, null, null, true);
        when(noteLockService.getLockStatus(1L, "testuser")).thenReturn(lockStatus);
        when(noteLockService.tryLockNote(1L, "testuser")).thenReturn(true);
        doNothing().when(noteLockService).unlockNote(1L, "testuser");


        when(noteService.updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser")))
                .thenReturn(updatedNote);

        // Act & Assert
        mockMvc.perform(put("/api/notes/1")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.note.titolo", is("Titolo Aggiornato")))
                .andExpect(jsonPath("$.note.contenuto", is("Contenuto aggiornato")));

        verify(noteService).updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser"));
    }


    @Test
    @DisplayName("UC4.C17 - Test endpoint PUT /notes/{id} senza permessi")
    @WithMockUser(username = "testuser")
    void testUpdateNoteWithoutPermissions() throws Exception {
        // Arrange
        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitolo("Tentativo");
        updateRequest.setContenuto("Contenuto valido");

        when(jwtUtil.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");

        // MOCK PER LOCK SERVICE
        LockStatusDto lockStatus = new LockStatusDto(false, null, null, true);
        when(noteLockService.getLockStatus(1L, "testuser")).thenReturn(lockStatus);
        when(noteLockService.tryLockNote(1L, "testuser")).thenReturn(true);


        when(noteService.updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser")))
                .thenThrow(new RuntimeException("Non hai i permessi per modificare questa nota"));

        // Act & Assert
        mockMvc.perform(put("/api/notes/1")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(csrf()))
                .andExpect(status().isForbidden());


        verify(noteService).updateNote(eq(1L), any(UpdateNoteRequest.class), eq("testuser"));
    }


    @Test
    @DisplayName("UC7.C18 - Test endpoint GET /notes/search")
    @WithMockUser(username = "testuser")
    void testSearchNotesEndpoint() throws Exception {
        // Arrange
        List<NoteDto> searchResults = Arrays.asList(
                createTestNoteDto(1L, "Found Note 1"),
                createTestNoteDto(2L, "Found Note 2")
        );

        when(jwtUtil.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(noteService.searchNotes("testuser", "test")).thenReturn(searchResults);

        // Act & Assert - USA IL PARAMETRO GIUSTO
        mockMvc.perform(get("/api/notes/search")
                        .param("q", "test")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", hasSize(2)))
                .andExpect(jsonPath("$.notes[0].titolo", is("Found Note 1")))
                .andExpect(jsonPath("$.notes[1].titolo", is("Found Note 2")));

        verify(noteService).searchNotes("testuser", "test");
    }


    @Test
    @DisplayName("UC7.C19 - Test endpoint GET /notes/filter/tag/{tag}")
    @WithMockUser(username = "testuser")
    void testGetNotesByTag() throws Exception {
        // Arrange
        List<NoteDto> tagResults = Arrays.asList(createTestNoteDto(1L, "Tagged Note"));

        when(jwtUtil.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(noteService.getNotesByTag("testuser", "important")).thenReturn(tagResults);


        mockMvc.perform(get("/api/notes/filter/tag/important")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo", is("Tagged Note")));

        verify(noteService).getNotesByTag("testuser", "important");
    }




    @Test
    @DisplayName("UC7.C20 - Test endpoint GET /notes/filter/cartella/{cartella}")
    @WithMockUser(username = "testuser")
    void testGetNotesByCartella() throws Exception {
        // Arrange
        List<NoteDto> cartellaResults = Arrays.asList(createTestNoteDto(1L, "Folder Note"));

        when(jwtUtil.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(noteService.getNotesByCartella("testuser", "work")).thenReturn(cartellaResults);


        mockMvc.perform(get("/api/notes/filter/cartella/work")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].titolo", is("Folder Note")));

        verify(noteService).getNotesByCartella("testuser", "work");
    }

    @Test
    @DisplayName("LOCK.C21 - Test endpoint POST /notes/{id}/lock")
    @WithMockUser(username = "testuser")
    void testLockNote() throws Exception {
        // Arrange
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(noteLockService.tryLockNote(1L, "testuser")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/notes/1/lock")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Nota bloccata per la modifica")));

        verify(noteLockService).tryLockNote(1L, "testuser");
    }

    @Test
    @DisplayName("LOCK.C22 - Test endpoint POST /notes/{id}/lock - conflitto")
    @WithMockUser(username = "testuser")
    void testLockNoteConflict() throws Exception {
        // Arrange
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(noteLockService.tryLockNote(1L, "testuser")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/notes/1/lock")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Nota già in modifica da other-user")));

        verify(noteLockService).tryLockNote(1L, "testuser");
    }

    @Test
    @DisplayName("LOCK.C23 - Test endpoint DELETE /notes/{id}/lock")
    @WithMockUser(username = "testuser")
    void testUnlockNote() throws Exception {
        // Arrange
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        doNothing().when(noteLockService).unlockNote(1L, "testuser");

        // Act & Assert
        mockMvc.perform(delete("/api/notes/1/lock")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Nota sbloccata con successo")));

        verify(noteLockService).unlockNote(1L, "testuser");
    }

    @Test
    @DisplayName("LOCK.C24 - Test endpoint GET /notes/{id}/lock-status")
    @WithMockUser(username = "testuser")
    void testGetLockStatus() throws Exception {
        // Arrange
        LockStatusDto lockStatus = new LockStatusDto(true, "otheruser",
                LocalDateTime.now().plusMinutes(5), false);

        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(noteLockService.getLockStatus(1L, "testuser")).thenReturn(lockStatus);

        // Act & Assert
        mockMvc.perform(get("/api/notes/1/lock-status")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked", is(true)))
                .andExpect(jsonPath("$.lockedBy", is("otheruser")))
                .andExpect(jsonPath("$.canEdit", is(false)));

        verify(noteLockService).getLockStatus(1L, "testuser");
    }

    @Test
    @DisplayName("UC5.C25 - Test endpoint POST /notes/{id}/duplicate")
    @WithMockUser(username = "testuser")
    void testDuplicateNote() throws Exception {
        // Arrange
        NoteDto duplicatedNote = createTestNoteDto(2L, "Original (Copia)");

        when(jwtUtil.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(noteService.duplicateNote(1L, "testuser")).thenReturn(duplicatedNote);


        mockMvc.perform(post("/api/notes/1/duplicate")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.note.titolo", is("Original (Copia)")))  // USA "note" NON "data"
                .andExpect(jsonPath("$.note.id", is(2)));  // USA "note" NON "data"

        verify(noteService).duplicateNote(1L, "testuser");
    }

    @Test
    @DisplayName("UC6.C26 - Test endpoint DELETE /notes/{id}")
    @WithMockUser(username = "testuser")
    void testDeleteNote() throws Exception {
        // Arrange
        when(jwtUtil.extractTokenFromHeader("Bearer valid-token")).thenReturn("valid-token");
        when(jwtUtil.isTokenValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");
        when(noteService.deleteNote(1L, "testuser")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/notes/1")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("eliminata")));

        verify(noteService).deleteNote(1L, "testuser");
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



    // Helper method
    private NoteDto createTestNoteDto(Long id, String titolo) {
        NoteDto note = new NoteDto();
        note.setId(id);
        note.setTitolo(titolo);
        note.setContenuto("Test content");
        note.setAutore("testuser");
        note.setTags(Set.of("test"));
        note.setCartelle(Set.of("test-folder"));
        note.setTipoPermesso(TipoPermesso.PRIVATA.name());
        note.setDataCreazione(LocalDateTime.now());
        note.setDataModifica(LocalDateTime.now());
        return note;
    }

    private void assertDoesNotThrow(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            throw new AssertionError("Expected no exception but got: " + e.getMessage(), e);
        }
    }



}