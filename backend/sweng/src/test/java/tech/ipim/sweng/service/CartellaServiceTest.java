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
import tech.ipim.sweng.dto.CreateCartellaRequest;
import tech.ipim.sweng.dto.UpdateCartellaRequest;
import tech.ipim.sweng.model.Cartella;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.CartellaRepository;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartellaServiceTest {

    @Mock
    private CartellaRepository cartellaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoteRepository noteRepository;

    @InjectMocks
    private CartellaService cartellaService;

    private User testUser;
    private Cartella testCartella;
    private CreateCartellaRequest createRequest;
    private UpdateCartellaRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123");
        testUser.setId(1L);

        testCartella = new Cartella("Test Cartella", testUser);
        testCartella.setId(1L);
        testCartella.setDescrizione("Descrizione di test");
        testCartella.setColore("#667eea");

        createRequest = new CreateCartellaRequest();
        createRequest.setNome("Nuova Cartella");
        createRequest.setDescrizione("Descrizione nuova cartella");
        createRequest.setColore("#ff6b6b");

        updateRequest = new UpdateCartellaRequest();
        updateRequest.setNome("Cartella Aggiornata");
        updateRequest.setDescrizione("Descrizione aggiornata");
        updateRequest.setColore("#4ecdc4");
    }

    @Test
    void shouldCreateCartellaSuccessfully() {

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartellaRepository.existsByNomeAndProprietario("Nuova Cartella", testUser)).thenReturn(false);
        when(cartellaRepository.save(any(Cartella.class))).thenReturn(testCartella);

        CartellaDto result = cartellaService.createCartella(createRequest, "testuser");

        assertThat(result).isNotNull();
        assertThat(result.getNome()).isEqualTo("Test Cartella");
        assertThat(result.getProprietario()).isEqualTo("testuser");
        assertThat(result.getNumeroNote()).isEqualTo(0);

        verify(userRepository).findByUsername("testuser");
        verify(cartellaRepository).existsByNomeAndProprietario("Nuova Cartella", testUser);
        verify(cartellaRepository).save(any(Cartella.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundForCreation() {

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartellaService.createCartella(createRequest, "nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: nonexistent");

        verify(cartellaRepository, never()).save(any(Cartella.class));
    }

    @Test
    void shouldThrowExceptionWhenCartellaAlreadyExists() {

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartellaRepository.existsByNomeAndProprietario("Nuova Cartella", testUser)).thenReturn(true);

        assertThatThrownBy(() -> cartellaService.createCartella(createRequest, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Esiste già una cartella con il nome: Nuova Cartella");

        verify(cartellaRepository, never()).save(any(Cartella.class));
    }

    @Test
    void shouldGetUserCartelle() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartellaRepository.findByProprietarioOrderByDataModificaDesc(testUser))
                .thenReturn(Arrays.asList(testCartella));
        when(noteRepository.findNotesByCartella("testuser", "Test Cartella"))
                .thenReturn(Arrays.asList(mock(Note.class), mock(Note.class))); // 2 note

        List<CartellaDto> result = cartellaService.getUserCartelle("testuser");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNome()).isEqualTo("Test Cartella");
        assertThat(result.get(0).getNumeroNote()).isEqualTo(2);

        verify(userRepository).findByUsername("testuser");
        verify(cartellaRepository).findByProprietarioOrderByDataModificaDesc(testUser);
    }

    @Test
    void shouldGetCartellaById() {
 
        when(cartellaRepository.findByIdAndUsername(1L, "testuser")).thenReturn(Optional.of(testCartella));
        when(noteRepository.findNotesByCartella("testuser", "Test Cartella")).thenReturn(Arrays.asList());


        Optional<CartellaDto> result = cartellaService.getCartellaById(1L, "testuser");


        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getNome()).isEqualTo("Test Cartella");

        verify(cartellaRepository).findByIdAndUsername(1L, "testuser");
    }

    @Test
    void shouldReturnEmptyWhenCartellaNotFound() {

        when(cartellaRepository.findByIdAndUsername(999L, "testuser")).thenReturn(Optional.empty());


        Optional<CartellaDto> result = cartellaService.getCartellaById(999L, "testuser");


        assertThat(result).isEmpty();
    }

    @Test
    void shouldUpdateCartellaSuccessfully() {
        when(cartellaRepository.findByIdAndUsername(1L, "testuser")).thenReturn(Optional.of(testCartella));
        when(cartellaRepository.existsByNomeAndProprietario("Cartella Aggiornata", testUser)).thenReturn(false);
        when(cartellaRepository.save(any(Cartella.class))).thenReturn(testCartella);
        when(noteRepository.findNotesByCartella("testuser", "Test Cartella")).thenReturn(Arrays.asList());

        CartellaDto result = cartellaService.updateCartella(1L, updateRequest, "testuser");

        assertThat(result).isNotNull();
        verify(cartellaRepository).findByIdAndUsername(1L, "testuser");
        verify(cartellaRepository).save(any(Cartella.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentCartella() {

        when(cartellaRepository.findByIdAndUsername(999L, "testuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartellaService.updateCartella(999L, updateRequest, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cartella non trovata o non accessibile");

        verify(cartellaRepository, never()).save(any(Cartella.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingWithExistingName() {
        when(cartellaRepository.findByIdAndUsername(1L, "testuser")).thenReturn(Optional.of(testCartella));
        when(cartellaRepository.existsByNomeAndProprietario("Cartella Aggiornata", testUser)).thenReturn(true);

        assertThatThrownBy(() -> cartellaService.updateCartella(1L, updateRequest, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Esiste già una cartella con il nome: Cartella Aggiornata");

        verify(cartellaRepository, never()).save(any(Cartella.class));
    }

    @Test
    void shouldDeleteEmptyCartellaSuccessfully() {
        when(cartellaRepository.findByIdAndUsername(1L, "testuser")).thenReturn(Optional.of(testCartella));
        when(noteRepository.findNotesByCartella("testuser", "Test Cartella")).thenReturn(Arrays.asList()); // Cartella vuota

        boolean result = cartellaService.deleteCartella(1L, "testuser");

        assertThat(result).isTrue();
        verify(cartellaRepository).delete(testCartella);
    }

    @Test
    void shouldThrowExceptionWhenDeletingCartellaWithNotes() {
        when(cartellaRepository.findByIdAndUsername(1L, "testuser")).thenReturn(Optional.of(testCartella));
        when(noteRepository.findNotesByCartella("testuser", "Test Cartella"))
                .thenReturn(Arrays.asList(mock(Note.class), mock(Note.class))); // Cartella con 2 note

        assertThatThrownBy(() -> cartellaService.deleteCartella(1L, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Impossibile eliminare la cartella: contiene 2 note. Sposta prima le note.");

        verify(cartellaRepository, never()).delete(any(Cartella.class));
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentCartella() {

        when(cartellaRepository.findByIdAndUsername(999L, "testuser")).thenReturn(Optional.empty());


        assertThatThrownBy(() -> cartellaService.deleteCartella(999L, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cartella non trovata o non accessibile");

        verify(cartellaRepository, never()).delete(any(Cartella.class));
    }

    @Test
    void shouldGetUserCartelleStats() {

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartellaRepository.countByProprietario(testUser)).thenReturn(3L);
        when(cartellaRepository.findByUsername("testuser")).thenReturn(Arrays.asList(
                createCartellaWithName("Lavoro"),
                createCartellaWithName("Personale"),
                createCartellaWithName("Studio")
        ));

   
        CartellaService.CartelleStats result = cartellaService.getUserCartelleStats("testuser");


        assertThat(result.getNumeroCartelle()).isEqualTo(3L);
        assertThat(result.getNomiCartelle()).containsExactly("Lavoro", "Personale", "Studio");

        verify(userRepository).findByUsername("testuser");
        verify(cartellaRepository).countByProprietario(testUser);
        verify(cartellaRepository).findByUsername("testuser");
    }

    @Test
    void shouldThrowExceptionWhenGettingStatsForNonExistentUser() {

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());


        assertThatThrownBy(() -> cartellaService.getUserCartelleStats("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: nonexistent");
    }

    @Test
    void shouldAllowUpdateWithSameName() {
        updateRequest.setNome("Test Cartella");
        when(cartellaRepository.findByIdAndUsername(1L, "testuser")).thenReturn(Optional.of(testCartella));
        when(cartellaRepository.save(any(Cartella.class))).thenReturn(testCartella);
        when(noteRepository.findNotesByCartella("testuser", "Test Cartella")).thenReturn(Arrays.asList());

        CartellaDto result = cartellaService.updateCartella(1L, updateRequest, "testuser");


        assertThat(result).isNotNull();
        verify(cartellaRepository, never()).existsByNomeAndProprietario(anyString(), any(User.class));
        verify(cartellaRepository).save(any(Cartella.class));
    }

    private Cartella createCartellaWithName(String nome) {
        Cartella cartella = new Cartella(nome, testUser);
        return cartella;
    }
}