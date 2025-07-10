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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * Test unitari per {@link CartellaService}.
 * <p>
 * Questi test verificano in isolamento il corretto funzionamento dei metodi
 * CRUD e statistici del servizio CartellaService.
 * <p>
 * Si testano sia i flussi positivi che le condizioni di errore (es. nome duplicato,
 * cartella inesistente, cartella non eliminabile se contiene note).
 * <p>
 * Le dipendenze dei repository vengono mockate per simulare i comportamenti
 * necessari ai test, isolando la logica di business.
 */

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

    /**
     * Setup iniziale prima di ogni test:
     * - Crea un utente di test con id e username
     * - Crea una cartella di test associata all'utente
     * - Prepara richieste di creazione e aggiornamento con dati predefiniti
     */

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

     /**
     * Testa la creazione corretta di una nuova cartella associata ad un utente esistente.
     *
     * Sequenza:
     * - Mock ricerca utente per username
     * - Mock controllo esistenza cartella con nome dato
     * - Mock salvataggio cartella
     * - Invoca il metodo createCartella del servizio
     *
     * Valida:
     * - CartellaDto restituita non nulla e con nome, proprietario corretti
     * - Numero note inizializzato a zero
     * - Verifica chiamate ai repository corrette
     */

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

    /**
     * Verifica che venga lanciata un'eccezione se si prova a creare una cartella
     * per un utente non esistente.
     *
     * Valida:
     * - Eccezione RuntimeException con messaggio specifico
     * - Nessun tentativo di salvataggio cartella effettuato
     */

    @Test
    void shouldThrowExceptionWhenUserNotFoundForCreation() {

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartellaService.createCartella(createRequest, "nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: nonexistent");

        verify(cartellaRepository, never()).save(any(Cartella.class));
    }

     /**
     * Verifica che la creazione di una cartella con nome già esistente per lo stesso utente
     * venga bloccata con eccezione.
     *
     * Valida:
     * - RuntimeException con messaggio appropriato
     * - Nessun salvataggio eseguito
     */

    @Test
    void shouldThrowExceptionWhenCartellaAlreadyExists() {

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cartellaRepository.existsByNomeAndProprietario("Nuova Cartella", testUser)).thenReturn(true);

        assertThatThrownBy(() -> cartellaService.createCartella(createRequest, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Esiste già una cartella con il nome: Nuova Cartella");

        verify(cartellaRepository, never()).save(any(Cartella.class));
    }

    /**
     * Verifica il recupero della lista di cartelle di un utente, includendo il
     * corretto conteggio delle note per ogni cartella.
     *
     * Sequenza:
     * - Mock ricerca utente
     * - Mock ritorno lista cartelle ordinate per data modifica
     * - Mock conteggio note per cartella
     *
     * Valida:
     * - Dimensione lista attesa
     * - Correttezza nome e conteggio note per ciascuna cartella
     * - Verifica chiamate ai repository
     */

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

    /**
     * Verifica il recupero di una cartella tramite id e username proprietario.
     *
     * Sequenza:
     * - Mock ricerca cartella per id e username
     * - Mock ricerca note della cartella
     *
     * Valida:
     * - Presenza dell'Optional con dati corretti
     * - Verifica chiamata al repository corretta
     *
     * @return Optional di CartellaDto presente se cartella trovata
     */

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

    /**
     * Verifica che venga restituito un Optional vuoto se la cartella
     * non viene trovata per id e username.
     *
     * @return Optional vuoto se cartella inesistente
     */

    @Test
    void shouldReturnEmptyWhenCartellaNotFound() {

        when(cartellaRepository.findByIdAndUsername(999L, "testuser")).thenReturn(Optional.empty());


        Optional<CartellaDto> result = cartellaService.getCartellaById(999L, "testuser");


        assertThat(result).isEmpty();
    }

    /**
     * Verifica aggiornamento corretto dei dati di una cartella esistente.
     *
     * Sequenza:
     * - Mock ricerca cartella esistente per id e username
     * - Mock verifica non esistenza di nome duplicato (se diverso dal corrente)
     * - Mock salvataggio aggiornamento cartella
     * - Mock ricerca note associate (per DTO)
     *
     * Valida:
     * - DTO aggiornato non nullo
     * - Verifica corrette chiamate ai repository
     *
     * @return DTO aggiornato della cartella
     */

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

    /**
     * Verifica il comportamento in caso di aggiornamento su cartella inesistente.
     *
     * Valida:
     * - Lancio RuntimeException con messaggio appropriato
     * - Nessun salvataggio eseguito
     */

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentCartella() {

        when(cartellaRepository.findByIdAndUsername(999L, "testuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartellaService.updateCartella(999L, updateRequest, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cartella non trovata o non accessibile");

        verify(cartellaRepository, never()).save(any(Cartella.class));
    }

    /**
     * Verifica il blocco dell'aggiornamento quando il nuovo nome della cartella
     * è già utilizzato da un'altra cartella dello stesso utente.
     *
     * Valida:
     * - RuntimeException con messaggio di nome duplicato
     * - Nessun salvataggio eseguito
     */

    @Test
    void shouldThrowExceptionWhenUpdatingWithExistingName() {
        when(cartellaRepository.findByIdAndUsername(1L, "testuser")).thenReturn(Optional.of(testCartella));
        when(cartellaRepository.existsByNomeAndProprietario("Cartella Aggiornata", testUser)).thenReturn(true);

        assertThatThrownBy(() -> cartellaService.updateCartella(1L, updateRequest, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Esiste già una cartella con il nome: Cartella Aggiornata");

        verify(cartellaRepository, never()).save(any(Cartella.class));
    }

    /**
     * Verifica la cancellazione di una cartella vuota (senza note).
     *
     * Sequenza:
     * - Mock ricerca cartella
     * - Mock lista note vuota per la cartella
     * - Invocazione metodo deleteCartella
     *
     * Valida:
     * - Metodo ritorna true indicando successo
     * - Chiamata a delete sul repository
     *
     * @return boolean true se cancellazione avvenuta
     */

    @Test
    void shouldDeleteEmptyCartellaSuccessfully() {
        when(cartellaRepository.findByIdAndUsername(1L, "testuser")).thenReturn(Optional.of(testCartella));
        when(noteRepository.findNotesByCartella("testuser", "Test Cartella")).thenReturn(Arrays.asList()); // Cartella vuota

        boolean result = cartellaService.deleteCartella(1L, "testuser");

        assertThat(result).isTrue();
        verify(cartellaRepository).delete(testCartella);
    }

    /**
     * Verifica il blocco della cancellazione di una cartella che contiene note.
     *
     * Valida:
     * - Lancio RuntimeException con messaggio che indica presenza note
     * - Nessuna cancellazione eseguita
     */

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

    /**
     * Verifica il comportamento di cancellazione quando la cartella non esiste.
     *
     * Valida:
     * - RuntimeException con messaggio specifico
     * - Nessuna cancellazione eseguita
     */

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentCartella() {

        when(cartellaRepository.findByIdAndUsername(999L, "testuser")).thenReturn(Optional.empty());


        assertThatThrownBy(() -> cartellaService.deleteCartella(999L, "testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cartella non trovata o non accessibile");

        verify(cartellaRepository, never()).delete(any(Cartella.class));
    }

    /**
     * Testa il recupero delle statistiche sulle cartelle di un utente,
     * inclusi numero totale e lista dei nomi.
     *
     * Sequenza:
     * - Mock ricerca utente
     * - Mock conteggio cartelle per utente
     * - Mock lista cartelle per username
     *
     * Valida:
     * - Statistiche corrette con numero e nomi attesi
     * - Verifica chiamate ai repository
     */

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

    /**
     * Verifica che venga lanciata eccezione nel caso in cui si chiedano
     * le statistiche di un utente inesistente.
     *
     * Valida:
     * - RuntimeException con messaggio utente non trovato
     */

    @Test
    void shouldThrowExceptionWhenGettingStatsForNonExistentUser() {

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());


        assertThatThrownBy(() -> cartellaService.getUserCartelleStats("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: nonexistent");
    }

    /**
     * Testa l'aggiornamento della cartella mantenendo lo stesso nome,
     * evitando inutili controlli di nome duplicato.
     *
     * Valida:
     * - DTO aggiornato non nullo
     * - Verifica che non venga chiamato existsByNomeAndProprietario
     * - Verifica salvataggio cartella
     */
    
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