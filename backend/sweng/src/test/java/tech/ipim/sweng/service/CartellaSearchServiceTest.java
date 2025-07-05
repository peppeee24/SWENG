package tech.ipim.sweng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tech.ipim.sweng.dto.CartellaDto;
import tech.ipim.sweng.model.Cartella;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.CartellaRepository;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartellaServiceSearchMethodsTest {

    @Mock
    private CartellaRepository cartellaRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartellaService cartellaService;

    private User testUser;
    private Cartella cartella1;
    private Cartella cartella2;
    private Note testNote;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123");
        testUser.setId(1L);

        cartella1 = new Cartella("Lavoro", testUser);
        cartella1.setId(1L);
        cartella1.setDescrizione("Cartella per note di lavoro");
        cartella1.setColore("#ff6b6b");
        cartella1.setDataCreazione(LocalDateTime.of(2024, 1, 15, 10, 0));
        cartella1.setDataModifica(LocalDateTime.of(2024, 1, 16, 12, 0));

        cartella2 = new Cartella("Personale", testUser);
        cartella2.setId(2L);
        cartella2.setDescrizione("Cartella per note personali");
        cartella2.setColore("#4ecdc4");
        cartella2.setDataCreazione(LocalDateTime.of(2024, 2, 10, 14, 0));
        cartella2.setDataModifica(LocalDateTime.of(2024, 2, 11, 16, 0));

        testNote = new Note("Test Note", "Test content", testUser);
        testNote.setId(1L);
        testNote.setCartelle(Set.of("Lavoro"));
    }

    @Test
    void shouldGetUserCartelle() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartellaRepository.findByProprietarioOrderByDataModificaDesc(testUser))
                .thenReturn(Arrays.asList(cartella1, cartella2));
        when(noteRepository.findNotesByCartella("testuser", "Lavoro"))
                .thenReturn(Arrays.asList(testNote, testNote)); // 2 note
        when(noteRepository.findNotesByCartella("testuser", "Personale"))
                .thenReturn(Arrays.asList(testNote)); // 1 nota

        // When
        List<CartellaDto> result = cartellaService.getUserCartelle("testuser");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNome()).isEqualTo("Lavoro");
        assertThat(result.get(0).getNumeroNote()).isEqualTo(2);
        assertThat(result.get(1).getNome()).isEqualTo("Personale");
        assertThat(result.get(1).getNumeroNote()).isEqualTo(1);

        verify(userRepository).findByUsername("testuser");
        verify(cartellaRepository).findByProprietarioOrderByDataModificaDesc(testUser);
        verify(noteRepository).findNotesByCartella("testuser", "Lavoro");
        verify(noteRepository).findNotesByCartella("testuser", "Personale");
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundForGetUserCartelle() {
        // Given
        when(userRepository.findByUsername("inesistente")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cartellaService.getUserCartelle("inesistente"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: inesistente");

        verify(userRepository).findByUsername("inesistente");
        verify(cartellaRepository, never()).findByProprietarioOrderByDataModificaDesc(any(User.class));
    }

    @Test
    void shouldGetCartellaById() {
        // Given
        when(cartellaRepository.findByIdAndUsername(1L, "testuser")).thenReturn(Optional.of(cartella1));
        when(noteRepository.findNotesByCartella("testuser", "Lavoro")).thenReturn(Arrays.asList(testNote));

        // When
        Optional<CartellaDto> result = cartellaService.getCartellaById(1L, "testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getNome()).isEqualTo("Lavoro");
        assertThat(result.get().getNumeroNote()).isEqualTo(1);

        verify(cartellaRepository).findByIdAndUsername(1L, "testuser");
        verify(noteRepository).findNotesByCartella("testuser", "Lavoro");
    }

    @Test
    void shouldReturnEmptyWhenCartellaNotFound() {
        // Given
        when(cartellaRepository.findByIdAndUsername(999L, "testuser")).thenReturn(Optional.empty());

        // When
        Optional<CartellaDto> result = cartellaService.getCartellaById(999L, "testuser");

        // Then
        assertThat(result).isEmpty();
        verify(cartellaRepository).findByIdAndUsername(999L, "testuser");
        verify(noteRepository, never()).findNotesByCartella(anyString(), anyString());
    }

    @Test
    void shouldGetUserCartelleStats() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartellaRepository.countByProprietario(testUser)).thenReturn(3L);
        when(cartellaRepository.findByUsername("testuser")).thenReturn(Arrays.asList(
                createCartellaWithName("Lavoro"),
                createCartellaWithName("Personale"),
                createCartellaWithName("Studio")
        ));

        // When
        CartellaService.CartelleStats result = cartellaService.getUserCartelleStats("testuser");

        // Then
        assertThat(result.getNumeroCartelle()).isEqualTo(3L);
        assertThat(result.getNomiCartelle()).containsExactly("Lavoro", "Personale", "Studio");

        verify(userRepository).findByUsername("testuser");
        verify(cartellaRepository).countByProprietario(testUser);
        verify(cartellaRepository).findByUsername("testuser");
    }

    @Test
    void shouldThrowExceptionWhenGettingStatsForNonExistentUser() {
        // Given
        when(userRepository.findByUsername("inesistente")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cartellaService.getUserCartelleStats("inesistente"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: inesistente");

        verify(userRepository).findByUsername("inesistente");
        verify(cartellaRepository, never()).countByProprietario(any(User.class));
    }

    @Test
    void shouldCalculateCorrectNumeroNoteInCartella() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartellaRepository.findByProprietarioOrderByDataModificaDesc(testUser))
                .thenReturn(Arrays.asList(cartella1));
        when(noteRepository.findNotesByCartella("testuser", "Lavoro"))
                .thenReturn(Arrays.asList(testNote, testNote, testNote)); // 3 note

        // When
        List<CartellaDto> result = cartellaService.getUserCartelle("testuser");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNumeroNote()).isEqualTo(3);
    }

    @Test
    void shouldHandleEmptyCartellaCorrectly() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartellaRepository.findByProprietarioOrderByDataModificaDesc(testUser))
                .thenReturn(Arrays.asList(cartella2));
        when(noteRepository.findNotesByCartella("testuser", "Personale"))
                .thenReturn(Collections.emptyList()); // Cartella vuota

        // When
        List<CartellaDto> result = cartellaService.getUserCartelle("testuser");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNumeroNote()).isEqualTo(0);
        assertThat(result.get(0).getNome()).isEqualTo("Personale");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoCartelle() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartellaRepository.findByProprietarioOrderByDataModificaDesc(testUser))
                .thenReturn(Collections.emptyList());

        // When
        List<CartellaDto> result = cartellaService.getUserCartelle("testuser");

        // Then
        assertThat(result).isEmpty();
        verify(cartellaRepository).findByProprietarioOrderByDataModificaDesc(testUser);
    }

    @Test
    void shouldCreateCartellaDto() {
        // Given
        when(cartellaRepository.findByIdAndUsername(1L, "testuser")).thenReturn(Optional.of(cartella1));
        when(noteRepository.findNotesByCartella("testuser", "Lavoro")).thenReturn(Arrays.asList(testNote));

        // When
        Optional<CartellaDto> result = cartellaService.getCartellaById(1L, "testuser");

        // Then
        assertThat(result).isPresent();
        CartellaDto dto = result.get();
        assertThat(dto.getId()).isEqualTo(cartella1.getId());
        assertThat(dto.getNome()).isEqualTo(cartella1.getNome());
        assertThat(dto.getDescrizione()).isEqualTo(cartella1.getDescrizione());
        assertThat(dto.getColore()).isEqualTo(cartella1.getColore());
        assertThat(dto.getProprietario()).isEqualTo(cartella1.getProprietario().getUsername());
        assertThat(dto.getDataCreazione()).isEqualTo(cartella1.getDataCreazione());
        assertThat(dto.getDataModifica()).isEqualTo(cartella1.getDataModifica());
        assertThat(dto.getNumeroNote()).isEqualTo(1);
    }

    @Test
    void shouldHandleCartelleStatsWithEmptyList() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartellaRepository.countByProprietario(testUser)).thenReturn(0L);
        when(cartellaRepository.findByUsername("testuser")).thenReturn(Collections.emptyList());

        // When
        CartellaService.CartelleStats result = cartellaService.getUserCartelleStats("testuser");

        // Then
        assertThat(result.getNumeroCartelle()).isEqualTo(0L);
        assertThat(result.getNomiCartelle()).isEmpty();
    }

    private Cartella createCartellaWithName(String nome) {
        Cartella cartella = new Cartella(nome, testUser);
        return cartella;
    }
}