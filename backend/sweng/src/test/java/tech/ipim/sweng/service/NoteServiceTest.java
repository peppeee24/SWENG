package tech.ipim.sweng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.NoteDto;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.dto.UpdateNoteRequest;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.dto.NoteVersionDto;
import tech.ipim.sweng.dto.VersionComparisonDto;
import tech.ipim.sweng.model.NoteVersion;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import tech.ipim.sweng.dto.PermissionDto;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;


@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private NoteVersionService noteVersionService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NoteService noteService;

    private User testUser;
    private Note testNote;
    private CreateNoteRequest createRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123");
        testUser.setId(1L);

        testNote = new Note("Test Note", "Test content", testUser);
        testNote.setId(1L);
        testNote.setTags(new HashSet<>(Set.of("test", "sample")));
        testNote.setCartelle(new HashSet<>(Set.of("Test Folder")));

        testNote.setTipoPermesso(TipoPermesso.PRIVATA);
        testNote.setPermessiLettura(new HashSet<>());
        testNote.setPermessiScrittura(new HashSet<>());

        createRequest = new CreateNoteRequest();
        createRequest.setTitolo("New Note");
        createRequest.setContenuto("New note content");
        createRequest.setTags(Set.of("new", "test"));
        createRequest.setCartelle(Set.of("New Folder"));

    }

    @Test
    void shouldCreateNoteSuccessfully() {
        // Setup dell'utente
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Setup della nota che verrà restituita dal mock
        Note savedNote = new Note("New Note", "New note content", testUser);
        savedNote.setId(1L);
        savedNote.setTags(new HashSet<>(Set.of("new", "test")));
        savedNote.setCartelle(new HashSet<>(Set.of("New Folder")));
        savedNote.setTipoPermesso(TipoPermesso.PRIVATA);
        savedNote.setPermessiLettura(new HashSet<>());
        savedNote.setPermessiScrittura(new HashSet<>());

        // CRITICAL: Mock per saveAndFlush invece di save
        when(noteRepository.saveAndFlush(any(Note.class))).thenReturn(savedNote);

        // Mock per il versionamento
        NoteVersion mockVersion = new NoteVersion(savedNote, 1, "New note content", "New Note", "testuser", "Creazione nota");
        when(noteVersionService.createVersion(any(Note.class), eq("testuser"), eq("Creazione nota")))
                .thenReturn(mockVersion);

        // Mock per il findById che viene chiamato alla fine per verifica
        when(noteRepository.findById(1L)).thenReturn(Optional.of(savedNote));

        // Esegui il test
        NoteDto result = noteService.createNote(createRequest, "testuser");

        // Verifica il risultato
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitolo()).isEqualTo("New Note");
        assertThat(result.getContenuto()).isEqualTo("New note content");
        assertThat(result.getAutore()).isEqualTo("testuser");

        // Verifica che i metodi siano stati chiamati
        verify(userRepository).findByUsername("testuser");
        verify(noteRepository).saveAndFlush(any(Note.class)); // Non save!
        verify(noteVersionService).createVersion(any(Note.class), eq("testuser"), eq("Creazione nota"));
    }


    @Test
    void shouldThrowExceptionWhenUserNotFoundForCreation() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote(createRequest, "nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: nonexistent");

        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void shouldGetAllAccessibleNotes() {
        List<Note> notes = Arrays.asList(testNote);
        when(noteRepository.findAllAccessibleNotes("testuser")).thenReturn(notes);

        List<NoteDto> result = noteService.getAllAccessibleNotes("testuser");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).isEqualTo("Test Note");

        verify(noteRepository).findAllAccessibleNotes("testuser");
    }

    @Test
    void shouldGetUserNotes() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(noteRepository.findByAutoreOrderByDataModificaDesc(testUser)).thenReturn(Arrays.asList(testNote));

        List<NoteDto> result = noteService.getUserNotes("testuser");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAutore()).isEqualTo("testuser");

        verify(userRepository).findByUsername("testuser");
        verify(noteRepository).findByAutoreOrderByDataModificaDesc(testUser);
    }

    @Test
    void shouldGetNoteById() {
        when(noteRepository.findAccessibleNoteById(1L, "testuser")).thenReturn(Optional.of(testNote));

        Optional<NoteDto> result = noteService.getNoteById(1L, "testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);

        verify(noteRepository).findAccessibleNoteById(1L, "testuser");
    }

    @Test
    void shouldReturnEmptyWhenNoteNotFound() {
        when(noteRepository.findAccessibleNoteById(999L, "testuser")).thenReturn(Optional.empty());

        Optional<NoteDto> result = noteService.getNoteById(999L, "testuser");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldSearchNotes() {
        when(noteRepository.searchNotesByKeyword("testuser", "test")).thenReturn(Arrays.asList(testNote));

        List<NoteDto> result = noteService.searchNotes("testuser", "test");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).isEqualTo("Test Note");

        verify(noteRepository).searchNotesByKeyword("testuser", "test");
    }

    @Test
    void shouldGetNotesByTag() {
        when(noteRepository.findNotesByTag("testuser", "test")).thenReturn(Arrays.asList(testNote));

        List<NoteDto> result = noteService.getNotesByTag("testuser", "test");

        assertThat(result).hasSize(1);
        verify(noteRepository).findNotesByTag("testuser", "test");
    }

    @Test
    void shouldGetNotesByCartella() {
        when(noteRepository.findNotesByCartella("testuser", "Test Folder")).thenReturn(Arrays.asList(testNote));

        List<NoteDto> result = noteService.getNotesByCartella("testuser", "Test Folder");

        assertThat(result).hasSize(1);
        verify(noteRepository).findNotesByCartella("testuser", "Test Folder");
    }

    @Test
    void shouldDuplicateNote() {
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Note duplicatedNote = new Note("Test Note (Copia)", "Test content", testUser);
        duplicatedNote.setId(2L);
        when(noteRepository.save(any(Note.class))).thenReturn(duplicatedNote);

        NoteDto result = noteService.duplicateNote(1L, "testuser");

        assertThat(result).isNotNull();
        assertThat(result.getTitolo()).isEqualTo("Test Note (Copia)");
        assertThat(result.getContenuto()).isEqualTo("Test content");

        verify(noteRepository).findById(1L);
        verify(userRepository).findByUsername("testuser");
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    void shouldThrowExceptionWhenDuplicatingNonExistentNote() {
        when(noteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.duplicateNote(1L, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Nota non trovata: 1");


        verify(noteRepository).findById(1L);
        verify(userRepository, never()).findByUsername(anyString());
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void shouldThrowExceptionWhenDuplicatingNonAccessibleNote() {
        User otherUser = new User("otheruser", "password456");
        Note inaccessibleNote = new Note("Inaccessible Note", "Content", otherUser);
        inaccessibleNote.setId(1L);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(inaccessibleNote));

        assertThatThrownBy(() -> noteService.duplicateNote(1L, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Non hai accesso a questa nota"); //  Messaggio che corrisponde al codice nel NoteService


        verify(noteRepository).findById(1L);
        verify(userRepository, never()).findByUsername(anyString());
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void shouldDeleteNoteWhenUserIsAuthor() {
        testNote.setAutore(testUser);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        boolean result = noteService.deleteNote(1L, "testuser");

        assertThat(result).isTrue();
        verify(noteRepository).delete(testNote);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNoteWithoutPermission() {
        User otherUser = new User("otheruser", "password");
        testNote.setAutore(otherUser);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        assertThatThrownBy(() -> noteService.deleteNote(1L, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Non hai i permessi per eliminare questa nota");

        verify(noteRepository, never()).delete(any(Note.class));
    }

    @Test
    void shouldGetUserStats() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(noteRepository.countByAutore(testUser)).thenReturn(5L);
        when(noteRepository.countSharedNotes("testuser")).thenReturn(3L);
        when(noteRepository.findAllTagsByUser("testuser")).thenReturn(Arrays.asList("tag1", "tag2"));
        when(noteRepository.findAllCartelleByUser("testuser")).thenReturn(Arrays.asList("folder1"));

        NoteService.UserStatsDto result = noteService.getUserStats("testuser"); // CORREZIONE: Uso della classe interna corretta

        assertThat(result.getNoteCreate()).isEqualTo(5L);
        assertThat(result.getNoteCondivise()).isEqualTo(3L);
        assertThat(result.getTagUtilizzati()).isEqualTo(2L);
        assertThat(result.getCartelleCreate()).isEqualTo(1L);
        assertThat(result.getAllTags()).containsExactly("tag1", "tag2");
        assertThat(result.getAllCartelle()).containsExactly("folder1");
    }

    @Test
    void shouldThrowExceptionWhenGettingStatsForNonExistentUser() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getUserStats("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: nonexistent");
    }

    // TEST PER RIMOZIONE DALLA CONDIVISIONE

    @Test
    @DisplayName("Dovrebbe rimuovere un utente dalla condivisione in lettura")
    void shouldRemoveUserFromReadingSharing() {
        // Arrange
        User owner = createTestUser("owner", "owner@test.com");

        Note note = createTestNote(owner);
        note.getPermessiLettura().add("shared");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        noteService.removeUserFromSharing(1L, "shared");

        // Assert
        assertFalse(note.getPermessiLettura().contains("shared"));
        verify(noteRepository).save(note);
        assertTrue(note.getDataModifica().isAfter(note.getDataCreazione()));
    }

    @Test
    @DisplayName("Dovrebbe rimuovere un utente dalla condivisione in scrittura")
    void shouldRemoveUserFromWritingSharing() {
        // Arrange
        User owner = createTestUser("owner", "owner@test.com");

        Note note = createTestNote(owner);
        note.getPermessiLettura().add("shared");
        note.getPermessiScrittura().add("shared");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        noteService.removeUserFromSharing(1L, "shared");

        // Assert
        assertFalse(note.getPermessiLettura().contains("shared"));
        assertFalse(note.getPermessiScrittura().contains("shared"));
        verify(noteRepository).save(note);
    }

    @Test
    @DisplayName("Dovrebbe fallire se l'utente è il proprietario")
    void shouldFailWhenUserIsOwner() {
        // Arrange
        User owner = createTestUser("owner", "owner@test.com");
        Note note = createTestNote(owner);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> noteService.removeUserFromSharing(1L, "owner"));

        assertEquals("Il proprietario non può rimuoversi dalla propria nota", exception.getMessage());
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    @DisplayName("Dovrebbe fallire se l'utente non ha accesso alla nota")
    void shouldFailWhenUserHasNoAccess() {
        // Arrange
        User owner = createTestUser("owner", "owner@test.com");
        Note note = createTestNote(owner);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> noteService.removeUserFromSharing(1L, "nonuser"));

        assertEquals("L'utente non ha accesso a questa nota", exception.getMessage());
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    @DisplayName("Dovrebbe fallire se la nota non esiste")
    void shouldFailWhenNoteNotFound() {
        // Arrange
        when(noteRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> noteService.removeUserFromSharing(999L, "user"));

        assertEquals("Nota non trovata", exception.getMessage());
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    @DisplayName("Dovrebbe aggiornare la data di modifica quando rimuove l'utente")
    void shouldUpdateModificationDateWhenRemovingUser() {
        // Arrange
        User owner = createTestUser("owner", "owner@test.com");
        Note note = createTestNote(owner);
        note.getPermessiLettura().add("shared");
        LocalDateTime originalModDate = note.getDataModifica();

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        noteService.removeUserFromSharing(1L, "shared");

        // Assert
        assertTrue(note.getDataModifica().isAfter(originalModDate));
    }

    @Test
    @DisplayName("Dovrebbe aggiornare una nota quando l'utente è il proprietario")
    void shouldUpdateNoteWhenUserIsOwner() {
        // Arrange
        User owner = createTestUser("owner", "owner@test.com");
        Note note = createTestNote(owner);
        note.setTags(Set.of("old-tag"));
        note.setCartelle(Set.of("old-folder"));

        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Titolo Aggiornato");
        request.setContenuto("Contenuto aggiornato");
        request.setTags(Set.of("new-tag", "updated"));
        request.setCartelle(Set.of("new-folder"));

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        NoteDto result = noteService.updateNote(1L, request, "owner");

        // Assert
        assertThat(result).isNotNull();
        assertThat(note.getTitolo()).isEqualTo("Titolo Aggiornato");
        assertThat(note.getContenuto()).isEqualTo("Contenuto aggiornato");
        assertThat(note.getTags()).containsExactlyInAnyOrder("new-tag", "updated");
        assertThat(note.getCartelle()).containsExactly("new-folder");
        assertTrue(note.getDataModifica().isAfter(note.getDataCreazione()));

        verify(noteRepository).findById(1L);
        verify(noteRepository).save(note);
    }

    @Test
    @DisplayName("Dovrebbe aggiornare una nota quando l'utente ha permessi di scrittura")
    void shouldUpdateNoteWhenUserHasWritePermission() {
        // Arrange
        User owner = createTestUser("owner", "owner@test.com");
        Note note = createTestNote(owner);
        note.getPermessiScrittura().add("editor");

        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Titolo Modificato");
        request.setContenuto("Contenuto modificato");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        NoteDto result = noteService.updateNote(1L, request, "editor");

        // Assert
        assertThat(result).isNotNull();
        assertThat(note.getTitolo()).isEqualTo("Titolo Modificato");
        assertThat(note.getContenuto()).isEqualTo("Contenuto modificato");

        verify(noteRepository).save(note);
    }

    @Test
    @DisplayName("Dovrebbe fallire quando l'utente non ha permessi di modifica")
    void shouldFailWhenUserHasNoEditPermission() {
        // Arrange
        User owner = createTestUser("owner", "owner@test.com");
        Note note = createTestNote(owner);

        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Tentativo Modifica");
        request.setContenuto("Non dovrebbe funzionare");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> noteService.updateNote(1L, request, "unauthorized"));

        assertEquals("Non hai i permessi per modificare questa nota", exception.getMessage());
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    @DisplayName("Dovrebbe fallire quando la nota non esiste")
    void shouldFailWhenNoteNotFoundForUpdate() {
        // Arrange
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Nota Inesistente");
        request.setContenuto("Non esiste");

        when(noteRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> noteService.updateNote(999L, request, "user"));

        assertEquals("Nota non trovata", exception.getMessage());
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    @DisplayName("Dovrebbe gestire tags e cartelle null")
    void shouldHandleNullTagsAndCartelle() {
        // Arrange
        User owner = createTestUser("owner", "owner@test.com");
        Note note = createTestNote(owner);
        note.setTags(Set.of("existing-tag"));
        note.setCartelle(Set.of("existing-folder"));

        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Titolo Senza Tags");
        request.setContenuto("Contenuto senza cartelle");
        request.setTags(null);
        request.setCartelle(null);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        NoteDto result = noteService.updateNote(1L, request, "owner");

        // Assert
        assertThat(result).isNotNull();
        assertThat(note.getTags()).isEmpty();
        assertThat(note.getCartelle()).isEmpty();

        verify(noteRepository).save(note);
    }

    @Test
    @DisplayName("Dovrebbe aggiornare solo i campi modificati")
    void shouldUpdateOnlyModifiedFields() {
        // Arrange
        User owner = createTestUser("owner", "owner@test.com");
        Note note = createTestNote(owner);
        note.setTags(Set.of("original-tag"));
        note.setCartelle(Set.of("original-folder"));
        LocalDateTime originalDate = note.getDataModifica();

        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("Solo Titolo Cambiato");
        request.setContenuto(note.getContenuto()); // Stesso contenuto
        request.setTags(note.getTags()); // Stessi tags
        request.setCartelle(note.getCartelle()); // Stesse cartelle

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        NoteDto result = noteService.updateNote(1L, request, "owner");

        // Assert
        assertThat(result).isNotNull();
        assertThat(note.getTitolo()).isEqualTo("Solo Titolo Cambiato");
        assertThat(note.getTags()).containsExactly("original-tag");
        assertThat(note.getCartelle()).containsExactly("original-folder");
        assertTrue(note.getDataModifica().isAfter(originalDate));

        verify(noteRepository).save(note);
    }

    @Test
    @DisplayName("Dovrebbe trimmare spazi bianchi dal titolo e contenuto")
    void shouldTrimWhitespaceFromTitleAndContent() {
        // Arrange
        User owner = createTestUser("owner", "owner@test.com");
        Note note = createTestNote(owner);

        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setTitolo("  Titolo con spazi  ");
        request.setContenuto("  Contenuto con spazi  ");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        noteService.updateNote(1L, request, "owner");

        // Assert
        assertThat(note.getTitolo()).isEqualTo("Titolo con spazi");
        assertThat(note.getContenuto()).isEqualTo("Contenuto con spazi");

        verify(noteRepository).save(note);
    }

    // METODI HELPER

    private User createTestUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setNome("Test");
        user.setCognome("User");
        return user;
    }

    private Note createTestNote(User author) {
        Note note = new Note();
        note.setId(1L);
        note.setTitolo("Test Note");
        note.setContenuto("Test content");
        note.setAutore(author);
        note.setDataCreazione(LocalDateTime.now().minusHours(1));
        note.setDataModifica(LocalDateTime.now().minusHours(1));
        note.setPermessiLettura(new HashSet<>());
        note.setPermessiScrittura(new HashSet<>());
        note.setTags(new HashSet<>());
        note.setCartelle(new HashSet<>());
        return note;
    }



    @Test
    @DisplayName("Dovrebbe ottenere la cronologia delle versioni per una nota accessibile")
    void shouldGetVersionHistoryForAccessibleNote() {
        // Given
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        NoteVersion version1 = new NoteVersion(testNote, 1, "Contenuto v1", "Titolo v1", "testuser", "Prima versione");
        NoteVersion version2 = new NoteVersion(testNote, 2, "Contenuto v2", "Titolo v2", "testuser", "Seconda versione");
        List<NoteVersion> versions = Arrays.asList(version2, version1);

        when(noteVersionService.getVersionHistory(1L)).thenReturn(versions);

        // When
        List<NoteVersionDto> result = noteService.getNoteVersionHistory(1L, "testuser");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersionNumber()).isEqualTo(2);
        assertThat(result.get(1).getVersionNumber()).isEqualTo(1);

        verify(noteRepository).findById(1L);
        verify(noteVersionService).getVersionHistory(1L);
    }

    @Test
    @DisplayName("Dovrebbe fallire l'ottenimento cronologia se nota non trovata")
    void shouldFailGetVersionHistoryIfNoteNotFound() {
        // Given
        when(noteRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteService.getNoteVersionHistory(999L, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Nota non trovata");

        verify(noteRepository).findById(999L);
        verify(noteVersionService, never()).getVersionHistory(anyLong());
    }

    @Test
    @DisplayName("Dovrebbe fallire l'ottenimento cronologia se utente non ha accesso")
    void shouldFailGetVersionHistoryIfUserHasNoAccess() {
        // Given
        Note privateNote = new Note("Nota Privata", "Contenuto Privato", testUser);
        privateNote.setId(1L);
        // Nota privata - solo l'autore ha accesso

        when(noteRepository.findById(1L)).thenReturn(Optional.of(privateNote));

        // When & Then
        assertThatThrownBy(() -> noteService.getNoteVersionHistory(1L, "altrouser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Non hai accesso a questa nota");

        verify(noteRepository).findById(1L);
        verify(noteVersionService, never()).getVersionHistory(anyLong());
    }

    @Test
    @DisplayName("Dovrebbe ottenere una versione specifica se accessibile")
    void shouldGetSpecificVersionIfAccessible() {
        // Given
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        NoteVersion version = new NoteVersion(testNote, 2, "Contenuto v2", "Titolo v2", "testuser", "Seconda versione");
        when(noteVersionService.getVersion(1L, 2)).thenReturn(Optional.of(version));

        // When
        Optional<NoteVersionDto> result = noteService.getNoteVersion(1L, 2, "testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getVersionNumber()).isEqualTo(2);
        assertThat(result.get().getContenuto()).isEqualTo("Contenuto v2");
        assertThat(result.get().getTitolo()).isEqualTo("Titolo v2");

        verify(noteRepository).findById(1L);
        verify(noteVersionService).getVersion(1L, 2);
    }

    @Test
    @DisplayName("Dovrebbe restituire vuoto se versione non esiste")
    void shouldReturnEmptyIfVersionNotExists() {
        // Given
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));
        when(noteVersionService.getVersion(1L, 999)).thenReturn(Optional.empty());

        // When
        Optional<NoteVersionDto> result = noteService.getNoteVersion(1L, 999, "testuser");

        // Then
        assertThat(result).isEmpty();

        verify(noteRepository).findById(1L);
        verify(noteVersionService).getVersion(1L, 999);
    }

    @Test
    @DisplayName("Dovrebbe ripristinare una versione precedente")
    void shouldRestorePreviousVersion() {
        // Given

        testNote.setTitolo("Titolo Corrente");
        testNote.setContenuto("Contenuto Corrente");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        NoteVersion versionToRestore = new NoteVersion(testNote, 2, "Contenuto v2", "Titolo v2", "testuser", "Seconda versione");
        when(noteVersionService.getVersion(1L, 2)).thenReturn(Optional.of(versionToRestore));
        when(noteRepository.save(any(Note.class))).thenReturn(testNote);

        NoteVersion newVersion = new NoteVersion(testNote, 4, "Contenuto v2", "Titolo v2", "testuser", "Ripristino alla versione 2");
        when(noteVersionService.createVersion(any(Note.class), eq("testuser"), anyString())).thenReturn(newVersion);

        // When
        NoteDto result = noteService.restoreNoteVersion(1L, 2, "testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitolo()).isEqualTo("Titolo v2");
        assertThat(result.getContenuto()).isEqualTo("Contenuto v2");

        verify(noteRepository).findById(1L);
        verify(noteVersionService).getVersion(1L, 2);
        verify(noteRepository).save(argThat(note ->
                note.getTitolo().equals("Titolo v2") &&
                        note.getContenuto().equals("Contenuto v2")
        ));
        verify(noteVersionService).createVersion(any(Note.class), eq("testuser"), contains("Ripristino alla versione 2"));
    }

    @Test
    @DisplayName("Dovrebbe fallire il ripristino se versione non esiste")
    void shouldFailRestoreIfVersionNotExists() {
        // Given
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));
        when(noteVersionService.getVersion(1L, 999)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteService.restoreNoteVersion(1L, 999, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Versione 999 non trovata");

        verify(noteRepository).findById(1L);
        verify(noteVersionService).getVersion(1L, 999);
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    @DisplayName("Dovrebbe fallire il ripristino se utente non ha accesso di scrittura")
    void shouldFailRestoreIfUserHasNoWriteAccess() {
        // Given
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));


        assertThatThrownBy(() -> noteService.restoreNoteVersion(1L, 2, "altrouser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Non hai i permessi per ripristinare versioni di questa nota"); // Messaggio corretto

        verify(noteRepository).findById(1L);
        verify(noteVersionService, never()).getVersion(anyLong(), anyInt());
    }


    @Test
    @DisplayName("Dovrebbe confrontare due versioni di una nota")
    void shouldCompareTwoVersionsOfNote() {
        // Given
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        NoteVersion version1 = new NoteVersion(testNote, 1, "Contenuto v1", "Titolo v1", "testuser", "Prima versione");
        NoteVersion version2 = new NoteVersion(testNote, 2, "Contenuto v2", "Titolo v2", "testuser", "Seconda versione");

        when(noteVersionService.getVersion(1L, 1)).thenReturn(Optional.of(version1));
        when(noteVersionService.getVersion(1L, 2)).thenReturn(Optional.of(version2));

        // When
        VersionComparisonDto result = noteService.compareNoteVersions(1L, 1, 2, "testuser");

        // Then
        assertThat(result).isNotNull();


        assertThat(result.getVersion1Number()).isEqualTo(1);
        assertThat(result.getVersion2Number()).isEqualTo(2);
        assertThat(result.getDifferences().isTitleChanged()).isTrue();
        assertThat(result.getDifferences().isContentChanged()).isTrue();

        verify(noteRepository).findById(1L);
        verify(noteVersionService).getVersion(1L, 1);
        verify(noteVersionService).getVersion(1L, 2);
    }



    @Test
    @DisplayName("Dovrebbe fallire il confronto se una delle versioni non esiste")
    void shouldFailCompareIfVersionNotExists() {
        // Given
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        NoteVersion version1 = new NoteVersion(testNote, 1, "Contenuto v1", "Titolo v1", "testuser", "Prima versione");
        when(noteVersionService.getVersion(1L, 1)).thenReturn(Optional.of(version1));
        when(noteVersionService.getVersion(1L, 999)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteService.compareNoteVersions(1L, 1, 999, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Versione 999 non trovata");

        verify(noteRepository).findById(1L);
        verify(noteVersionService).getVersion(1L, 1);
        verify(noteVersionService).getVersion(1L, 999);
    }

    @Test
    @DisplayName("Dovrebbe creare una versione quando si aggiorna una nota")
    void shouldCreateVersionWhenUpdatingNote() {
        // Given
        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitolo("Titolo Aggiornato");
        updateRequest.setContenuto("Contenuto Aggiornato");
        updateRequest.setTags(Set.of("tag1", "tag2"));

        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));
        when(noteRepository.save(any(Note.class))).thenReturn(testNote);

        NoteVersion newVersion = new NoteVersion(testNote, 2, "Contenuto Aggiornato", "Titolo Aggiornato", "testuser", "Aggiornamento contenuto");
        when(noteVersionService.createVersion(any(Note.class), eq("testuser"), anyString())).thenReturn(newVersion);

        // When
        NoteDto result = noteService.updateNote(1L, updateRequest, "testuser");

        // Then
        assertThat(result).isNotNull();

        verify(noteRepository).findById(1L);
        verify(noteRepository).save(any(Note.class));
        verify(noteVersionService).createVersion(any(Note.class), eq("testuser"), anyString());
    }

    @Test
    @DisplayName("Dovrebbe creare una versione quando si aggiornano i permessi")
    void shouldCreateVersionWhenUpdatingPermissions() {
        // Setup della nota esistente con permessi inizializzati
        testNote.setTipoPermesso(TipoPermesso.PRIVATA);
        testNote.setPermessiLettura(new HashSet<>());
        testNote.setPermessiScrittura(new HashSet<>());

        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        // Setup della nota aggiornata
        Note updatedNote = new Note(testNote.getTitolo(), testNote.getContenuto(), testNote.getAutore());
        updatedNote.setId(1L);
        updatedNote.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA);
        updatedNote.setPermessiLettura(new HashSet<>(Arrays.asList("user1", "user2")));
        updatedNote.setPermessiScrittura(new HashSet<>());

        // Mock per saveAndFlush (metodo usato in updateNotePermissions)
        when(noteRepository.saveAndFlush(any(Note.class))).thenReturn(updatedNote);

        // Mock per la creazione della versione
        NoteVersion newVersion = new NoteVersion(updatedNote, 2, updatedNote.getContenuto(),
                updatedNote.getTitolo(), "testuser", "Modifica permessi");
        when(noteVersionService.createVersion(any(Note.class), eq("testuser"), eq("Modifica permessi")))
                .thenReturn(newVersion);

        // Setup del PermissionDto
        PermissionDto permissionDto = new PermissionDto();
        permissionDto.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA);
        permissionDto.setUtentiLettura(Arrays.asList("user1", "user2"));
        permissionDto.setUtentiScrittura(Arrays.asList());

        // Esegui il test
        NoteDto result = noteService.updateNotePermissions(1L, permissionDto, "testuser");

        // Verifica il risultato
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTipoPermesso()).isEqualTo("CONDIVISA_LETTURA");

        // Verifica che i metodi siano stati chiamati
        verify(noteRepository).findById(1L);
        verify(noteRepository).saveAndFlush(any(Note.class));
        verify(noteVersionService).createVersion(any(Note.class), eq("testuser"), eq("Modifica permessi"));
    }



    @Test
    @DisplayName("Dovrebbe eliminare tutte le versioni quando si elimina una nota")
    void shouldDeleteAllVersionsWhenDeletingNote() {
        // Given
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        // When
        noteService.deleteNote(1L, "testuser");

        // Then
        verify(noteRepository).findById(1L);
        verify(noteVersionService).deleteAllVersionsForNote(1L);
        verify(noteRepository).delete(testNote);
    }
}