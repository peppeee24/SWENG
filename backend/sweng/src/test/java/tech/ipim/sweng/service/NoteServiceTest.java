package tech.ipim.sweng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

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
        testNote.setTags(Set.of("test", "sample"));
        testNote.setCartelle(Set.of("Test Folder"));

        createRequest = new CreateNoteRequest();
        createRequest.setTitolo("New Note");
        createRequest.setContenuto("New note content");
        createRequest.setTags(Set.of("new", "test"));
        createRequest.setCartelle(Set.of("New Folder"));
    }

    @Test
    void shouldCreateNoteSuccessfully() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(noteRepository.save(any(Note.class))).thenReturn(testNote);

        NoteDto result = noteService.createNote(createRequest, "testuser");

        assertThat(result).isNotNull();
        assertThat(result.getTitolo()).isEqualTo("Test Note");
        assertThat(result.getContenuto()).isEqualTo("Test content");
        assertThat(result.getAutore()).isEqualTo("testuser");

        verify(userRepository).findByUsername("testuser");
        verify(noteRepository).save(any(Note.class));
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
        // Mock findById (primo controllo nel service)
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));
        // Mock findByUsername per l'autore
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
        // Mock che la nota non esiste nel database
        when(noteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.duplicateNote(1L, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Nota non trovata nel database");

        verify(noteRepository).findById(1L);
        verify(userRepository, never()).findByUsername(anyString());
        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void shouldThrowExceptionWhenDuplicatingNonAccessibleNote() {
        // Crea un'altra nota con un altro utente (non accessibile a testuser)
        User otherUser = new User("otheruser", "password456");
        Note inaccessibleNote = new Note("Inaccessible Note", "Content", otherUser);
        inaccessibleNote.setId(1L);
        
        // Mock che la nota esiste ma non è accessibile
        when(noteRepository.findById(1L)).thenReturn(Optional.of(inaccessibleNote));

        assertThatThrownBy(() -> noteService.duplicateNote(1L, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Non hai i permessi per duplicare questa nota");

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

        NoteService.UserNotesStats result = noteService.getUserStats("testuser");

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
}