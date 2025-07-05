package tech.ipim.sweng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tech.ipim.sweng.dto.NoteDto;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.service.NoteVersionService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NoteServiceSearchMethodsTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoteVersionService noteVersionService;

    @InjectMocks
    private NoteService noteService;

    private User testUser;
    private User otherUser;
    private Note testNote1;
    private Note testNote2;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123");
        testUser.setId(1L);

        otherUser = new User("otheruser", "password456");
        otherUser.setId(2L);

        testNote1 = new Note("Prima Nota Java", "Contenuto sulla programmazione Java", testUser);
        testNote1.setId(1L);
        testNote1.setDataCreazione(LocalDateTime.of(2024, 1, 15, 10, 0));
        testNote1.setDataModifica(LocalDateTime.of(2024, 1, 16, 12, 0));
        testNote1.setCartelle(Set.of("Programmazione"));

        testNote2 = new Note("Seconda Nota Python", "Contenuto sulla programmazione Python", otherUser);
        testNote2.setId(2L);
        testNote2.setDataCreazione(LocalDateTime.of(2024, 2, 10, 14, 0));
        testNote2.setDataModifica(LocalDateTime.of(2024, 2, 11, 16, 0));
        testNote2.setCartelle(Set.of("Programmazione"));
    }

    @Test
    void shouldSearchNotesByKeyword() {
        // Given
        when(noteRepository.searchNotesByKeyword("testuser", "Java")).thenReturn(Arrays.asList(testNote1));

        // When
        List<NoteDto> result = noteService.searchNotes("testuser", "Java");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).isEqualTo("Prima Nota Java");
        verify(noteRepository).searchNotesByKeyword("testuser", "Java");
    }

    @Test
    void shouldReturnEmptyListWhenNoNotesFoundByKeyword() {
        // Given
        when(noteRepository.searchNotesByKeyword("testuser", "inesistente")).thenReturn(Collections.emptyList());

        // When
        List<NoteDto> result = noteService.searchNotes("testuser", "inesistente");

        // Then
        assertThat(result).isEmpty();
        verify(noteRepository).searchNotesByKeyword("testuser", "inesistente");
    }

    @Test
    void shouldSearchNotesByTag() {
        // Given
        when(noteRepository.findNotesByTag("testuser", "importante")).thenReturn(Arrays.asList(testNote1));

        // When
        List<NoteDto> result = noteService.getNotesByTag("testuser", "importante");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(noteRepository).findNotesByTag("testuser", "importante");
    }

    @Test
    void shouldReturnEmptyListWhenNoNotesFoundByTag() {
        // Given
        when(noteRepository.findNotesByTag("testuser", "inesistente")).thenReturn(Collections.emptyList());

        // When
        List<NoteDto> result = noteService.getNotesByTag("testuser", "inesistente");

        // Then
        assertThat(result).isEmpty();
        verify(noteRepository).findNotesByTag("testuser", "inesistente");
    }

    @Test
    void shouldSearchNotesByCartella() {
        // Given
        when(noteRepository.findNotesByCartella("testuser", "Programmazione")).thenReturn(Arrays.asList(testNote1));

        // When
        List<NoteDto> result = noteService.getNotesByCartella("testuser", "Programmazione");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCartelle()).contains("Programmazione");
        verify(noteRepository).findNotesByCartella("testuser", "Programmazione");
    }

    @Test
    void shouldReturnEmptyListWhenNoNotesInCartella() {
        // Given
        when(noteRepository.findNotesByCartella("testuser", "Vuota")).thenReturn(Collections.emptyList());

        // When
        List<NoteDto> result = noteService.getNotesByCartella("testuser", "Vuota");

        // Then
        assertThat(result).isEmpty();
        verify(noteRepository).findNotesByCartella("testuser", "Vuota");
    }

    @Test
    void shouldSearchNotesByAutoreWithOwnFilter() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(noteRepository.findByAutoreOrderByDataModificaDesc(testUser)).thenReturn(Arrays.asList(testNote1));

        // When
        List<NoteDto> result = noteService.getNotesByAutore("testuser", "testuser", "own");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAutore()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
        verify(noteRepository).findByAutoreOrderByDataModificaDesc(testUser);
    }

    @Test
    void shouldSearchNotesByAutoreWithSharedFilter() {
        // Given
        when(noteRepository.findSharedNotesByAutore("testuser", "otheruser")).thenReturn(Arrays.asList(testNote2));

        // When
        List<NoteDto> result = noteService.getNotesByAutore("testuser", "otheruser", "shared");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAutore()).isEqualTo("otheruser");
        verify(noteRepository).findSharedNotesByAutore("testuser", "otheruser");
    }

    @Test
    void shouldSearchNotesByAutoreWithAllFilter() {
        // Given
        when(noteRepository.findAccessibleNotesByAutore("testuser", "otheruser")).thenReturn(Arrays.asList(testNote2));

        // When
        List<NoteDto> result = noteService.getNotesByAutore("testuser", "otheruser", "all");

        // Then
        assertThat(result).hasSize(1);
        verify(noteRepository).findAccessibleNotesByAutore("testuser", "otheruser");
    }

    @Test
    void shouldSearchNotesByDateRangeWithOwnFilter() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(noteRepository.findByAutoreOrderByDataModificaDesc(testUser)).thenReturn(Arrays.asList(testNote1));

        // When
        List<NoteDto> result = noteService.getNotesByDateRange("testuser", "2024-01-01", "2024-01-31", "own");

        // Then
        assertThat(result).hasSize(1);
        verify(userRepository).findByUsername("testuser");
        verify(noteRepository).findByAutoreOrderByDataModificaDesc(testUser);
    }

    @Test
    void shouldSearchNotesByDateRangeWithSharedFilter() {
        // Given
        when(noteRepository.findSharedNotesByDateRange(eq("testuser"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testNote2));

        // When
        List<NoteDto> result = noteService.getNotesByDateRange("testuser", "2024-01-01", "2024-01-31", "shared");

        // Then
        assertThat(result).hasSize(1);
        verify(noteRepository).findSharedNotesByDateRange(eq("testuser"), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldSearchNotesByDateRangeWithAllFilter() {
        // Given
        when(noteRepository.findAccessibleNotesByDateRange(eq("testuser"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testNote1, testNote2));

        // When
        List<NoteDto> result = noteService.getNotesByDateRange("testuser", "2024-01-01", "2024-01-31", "all");

        // Then
        assertThat(result).hasSize(2);
        verify(noteRepository).findAccessibleNotesByDateRange(eq("testuser"), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldHandleNullDateRangeForOwnFilter() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(noteRepository.findByAutoreOrderByDataModificaDesc(testUser)).thenReturn(Arrays.asList(testNote1));

        // When
        List<NoteDto> result = noteService.getNotesByDateRange("testuser", null, null, "own");

        // Then
        assertThat(result).hasSize(1);
        verify(noteRepository).findByAutoreOrderByDataModificaDesc(testUser);
    }

    @Test
    void shouldHandleNullDateRangeForAllFilter() {
        // Given
        when(noteRepository.findAllAccessibleNotes("testuser")).thenReturn(Arrays.asList(testNote1, testNote2));

        // When
        List<NoteDto> result = noteService.getNotesByDateRange("testuser", null, null, "all");

        // Then
        assertThat(result).hasSize(2);
        verify(noteRepository).findAllAccessibleNotes("testuser");
    }

    @Test
    void shouldGetAllAccessibleNotes() {
        // Given
        when(noteRepository.findAllAccessibleNotes("testuser")).thenReturn(Arrays.asList(testNote1, testNote2));

        // When
        List<NoteDto> result = noteService.getAllAccessibleNotes("testuser");

        // Then
        assertThat(result).hasSize(2);
        verify(noteRepository).findAllAccessibleNotes("testuser");
    }

    @Test
    void shouldGetUserNotes() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(noteRepository.findByAutoreOrderByDataModificaDesc(testUser)).thenReturn(Arrays.asList(testNote1));

        // When
        List<NoteDto> result = noteService.getUserNotes("testuser");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAutore()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
        verify(noteRepository).findByAutoreOrderByDataModificaDesc(testUser);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundForUserNotes() {
        // Given
        when(userRepository.findByUsername("inesistente")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> noteService.getUserNotes("inesistente"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: inesistente");

        verify(userRepository).findByUsername("inesistente");
        verify(noteRepository, never()).findByAutoreOrderByDataModificaDesc(any(User.class));
    }

    @Test
    void shouldGetNoteById() {
        // Given
        when(noteRepository.findAccessibleNoteById(1L, "testuser")).thenReturn(Optional.of(testNote1));

        // When
        Optional<NoteDto> result = noteService.getNoteById(1L, "testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(noteRepository).findAccessibleNoteById(1L, "testuser");
    }

    @Test
    void shouldReturnEmptyWhenNoteNotFound() {
        // Given
        when(noteRepository.findAccessibleNoteById(999L, "testuser")).thenReturn(Optional.empty());

        // When
        Optional<NoteDto> result = noteService.getNoteById(999L, "testuser");

        // Then
        assertThat(result).isEmpty();
        verify(noteRepository).findAccessibleNoteById(999L, "testuser");
    }

    @Test
    void shouldTrimKeywordInSearch() {
        // Given
        when(noteRepository.searchNotesByKeyword("testuser", "Java")).thenReturn(Arrays.asList(testNote1));

        // When
        List<NoteDto> result = noteService.searchNotes("testuser", "  Java  ");

        // Then
        assertThat(result).hasSize(1);
        verify(noteRepository).searchNotesByKeyword("testuser", "Java");
    }
}