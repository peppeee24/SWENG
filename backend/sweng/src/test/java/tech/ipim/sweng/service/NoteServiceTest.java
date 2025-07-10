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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import tech.ipim.sweng.dto.PermissionDto;

/**
 * Test di unità per la classe NoteService.
 * 
 * Questa classe verifica il corretto funzionamento delle operazioni principali
 * sulle note, inclusi aggiornamenti, gestione permessi, versionamento, confronto
 * versioni e cancellazione. 
 * 
 * Si usano mock per il repository delle note e il servizio di versionamento
 * per isolare la logica di business e testare il comportamento in diversi scenari,
 * come modifiche parziali, gestione permessi e ripristino di versioni precedenti.
 */

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

    /**
     * Test di creazione di una nuova nota con utente valido.
     * <p>
     * Mocka il repository utente per trovare l'utente esistente con username "testuser".
     * Mocka il repository note per salvare e flushare la nuova nota, simulando il salvataggio nel DB.
     * Mocka il servizio di versionamento per creare la prima versione della nota con descrizione "Creazione nota".
     * Mocka il findById per verificare la presenza della nota salvata.
     * Verifica che la nota restituita dal servizio abbia i campi corretti, incluso l'autore,
     * e che i metodi repository e versionamento siano chiamati correttamente con i parametri attesi.
     */

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

    /**
     * Test che verifica il comportamento quando l'utente per la creazione della nota non viene trovato.
     * <p>
     * Mocka il repository utente per restituire Optional.empty() per uno username inesistente.
     * Verifica che il metodo createNote lanci un'eccezione RuntimeException con messaggio appropriato.
     * Assicura che il repository note non venga mai chiamato a salvare la nota.
     */

    @Test
    void shouldThrowExceptionWhenUserNotFoundForCreation() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.createNote(createRequest, "nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: nonexistent");

        verify(noteRepository, never()).save(any(Note.class));
    }

    /**
     * Test per recuperare tutte le note accessibili da un dato utente.
     * <p>
     * Mocka il repository note per restituire una lista contenente la nota di test.
     * Verifica che la lista restituita dal servizio contenga esattamente una nota
     * e che il titolo corrisponda a quello atteso.
     * Controlla che il metodo findAllAccessibleNotes sia chiamato una volta con il corretto username.
     */

    @Test
    void shouldGetAllAccessibleNotes() {
        List<Note> notes = Arrays.asList(testNote);
        when(noteRepository.findAllAccessibleNotes("testuser")).thenReturn(notes);

        List<NoteDto> result = noteService.getAllAccessibleNotes("testuser");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).isEqualTo("Test Note");

        verify(noteRepository).findAllAccessibleNotes("testuser");
    }

    /**
     * Test per ottenere tutte le note create da un utente specifico.
     * <p>
     * Mocka il repository utente per restituire l'utente di test con username "testuser".
     * Mocka il repository note per restituire la lista delle note di quell'autore ordinate per data modifica decrescente.
     * Verifica che la lista contenga la nota di test con l'autore corretto.
     * Controlla che entrambi i repository vengano interrogati con i parametri corretti.
     */

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

    /**
     * Test per ottenere una singola nota accessibile tramite ID e username.
     * <p>
     * Mocka il repository note per restituire la nota di test in un Optional.
     * Verifica che il risultato del servizio sia presente e che l'ID corrisponda a quello atteso.
     * Assicura che il metodo findAccessibleNoteById sia chiamato con i parametri corretti.
     */

    @Test
    void shouldGetNoteById() {
        when(noteRepository.findAccessibleNoteById(1L, "testuser")).thenReturn(Optional.of(testNote));

        Optional<NoteDto> result = noteService.getNoteById(1L, "testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);

        verify(noteRepository).findAccessibleNoteById(1L, "testuser");
    }

    /**
     * Test che verifica il comportamento nel caso in cui la nota con l'ID specificato non esista.
     * <p>
     * Mocka il repository note per restituire Optional.empty().
     * Verifica che il servizio restituisca un Optional vuoto senza eccezioni.
     */

    @Test
    void shouldReturnEmptyWhenNoteNotFound() {
        when(noteRepository.findAccessibleNoteById(999L, "testuser")).thenReturn(Optional.empty());

        Optional<NoteDto> result = noteService.getNoteById(999L, "testuser");

        assertThat(result).isEmpty();
    }

    /**
     * Test per la ricerca di note tramite keyword.
     * <p>
     * Mocka il repository note per restituire una lista contenente la nota di test filtrata per keyword.
     * Verifica che la lista risultante abbia dimensione uno e il titolo corrispondente.
     * Controlla la chiamata del repository con i parametri corretti.
     */

    @Test
    void shouldSearchNotes() {
        when(noteRepository.searchNotesByKeyword("testuser", "test")).thenReturn(Arrays.asList(testNote));

        List<NoteDto> result = noteService.searchNotes("testuser", "test");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).isEqualTo("Test Note");

        verify(noteRepository).searchNotesByKeyword("testuser", "test");
    }

    /**
     * Test per il recupero di note filtrate tramite tag.
     * <p>
     * Mocka il repository per restituire la lista di note che contengono il tag specificato.
     * Verifica che la lista risultante contenga la nota di test.
     * Controlla la corretta chiamata al repository con username e tag.
     */
    @Test
    void shouldGetNotesByTag() {
        when(noteRepository.findNotesByTag("testuser", "test")).thenReturn(Arrays.asList(testNote));

        List<NoteDto> result = noteService.getNotesByTag("testuser", "test");

        assertThat(result).hasSize(1);
        verify(noteRepository).findNotesByTag("testuser", "test");
    }

    /**
     * Test per il recupero di note filtrate tramite cartella.
     * <p>
     * Mocka il repository per restituire la lista di note nella cartella specificata.
     * Verifica che la lista risultante contenga la nota di test.
     * Controlla la chiamata del repository con i parametri attesi.
     */

    @Test
    void shouldGetNotesByCartella() {
        when(noteRepository.findNotesByCartella("testuser", "Test Folder")).thenReturn(Arrays.asList(testNote));

        List<NoteDto> result = noteService.getNotesByCartella("testuser", "Test Folder");

        assertThat(result).hasSize(1);
        verify(noteRepository).findNotesByCartella("testuser", "Test Folder");
    }

    /**
     * Test di duplicazione di una nota esistente da parte di un utente autorizzato.
     * <p>
     * Mocka il repository note per trovare la nota originale tramite ID.
     * Mocka il repository utente per trovare l'utente corrente.
     * Mocka il salvataggio della nuova nota duplicata.
     * Verifica che la nota duplicata abbia titolo aggiornato (con suffisso " (Copia)") e contenuto uguale.
     * Controlla che tutti i metodi repository vengano chiamati con i parametri corretti.
     */

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

    /**
     * Test che verifica che venga lanciata un'eccezione quando si tenta di duplicare
     * una nota inesistente.
     * <p>
     * Mocka il repository note per restituire Optional.empty().
     * Verifica che il servizio lanci RuntimeException con messaggio adeguato.
     * Assicura che i metodi di repository user e save non vengano mai invocati.
     */
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

    /**
     * Test che verifica che venga lanciata un'eccezione quando si tenta di duplicare
     * una nota inesistente.
     * <p>
     * Mocka il repository note per restituire Optional.empty().
     * Verifica che il servizio lanci RuntimeException con messaggio adeguato.
     * Assicura che i metodi di repository user e save non vengano mai invocati.
     */

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


    /**
     * Test di cancellazione di una nota da parte dell'autore.
     * <p>
     * Mocka il repository note per restituire la nota di test.
     * Verifica che la cancellazione ritorni true e che il repository venga chiamato per eliminare la nota.
     */
    @Test
    void shouldDeleteNoteWhenUserIsAuthor() {
        testNote.setAutore(testUser);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));

        boolean result = noteService.deleteNote(1L, "testuser");

        assertThat(result).isTrue();
        verify(noteRepository).delete(testNote);
    }

    /**
     * Test che verifica il fallimento della cancellazione quando l'utente non ha i permessi.
     * <p>
     * Mocka il repository note per restituire una nota il cui autore è un altro utente.
     * Verifica che il servizio lanci RuntimeException con messaggio di permessi negati.
     * Controlla che il metodo delete del repository non venga mai invocato.
     */

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

    /**
     * Test per ottenere statistiche relative a un utente.
     * <p>
     * Mocka il repository utente per restituire l'utente di test.
     * Mocka il repository note per restituire conteggi di note create, condivise, tag e cartelle.
     * Verifica che i valori del DTO restituito corrispondano ai dati mockati.
     */

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

    /**
     * Test che verifica che venga lanciata un'eccezione quando si richiedono
     * statistiche per un utente non esistente.
     * <p>
     * Mocka il repository utente per restituire Optional.empty().
     * Verifica che venga lanciata RuntimeException con messaggio corretto.
     */

    @Test
    void shouldThrowExceptionWhenGettingStatsForNonExistentUser() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getUserStats("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Utente non trovato: nonexistent");
    }

    // TEST PER RIMOZIONE DALLA CONDIVISIONE

    /**
     * Test di rimozione di un utente dalla condivisione in sola lettura di una nota.
     * <p>
     * Prepara una nota con permessi di lettura che includono l'utente da rimuovere.
     * Mocka il repository per trovare la nota e salvarla.
     * Chiama il servizio per rimuovere l'utente dalla condivisione.
     * Verifica che i permessi di lettura siano aggiornati e che la data di modifica sia aggiornata.
     * Controlla che il repository venga chiamato per salvare la nota modificata.
     */
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

    /**
     * Test di rimozione di un utente dalla condivisione in scrittura (e lettura) di una nota.
     * <p>
     * Prepara una nota con permessi di lettura e scrittura che includono l'utente da rimuovere.
     * Mocka il repository per trovare e salvare la nota.
     * Chiama il servizio per rimuovere l'utente.
     * Verifica che i permessi di lettura e scrittura non includano più l'utente.
     * Controlla la chiamata al repository per il salvataggio.
     */

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

    /**
     * Test che verifica il fallimento della rimozione quando l'utente da rimuovere
     * è il proprietario della nota.
     * <p>
     * Prepara una nota con proprietario uguale all'utente da rimuovere.
     * Mocka il repository per trovare la nota.
     * Verifica che venga lanciata RuntimeException con messaggio di impossibilità di rimuovere il proprietario.
     * Controlla che il repository non esegua salvataggi.
     */
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

    /**
     * Test che verifica il fallimento della rimozione quando l'utente
     * non ha alcun accesso alla nota.
     * <p>
     * Prepara una nota con un proprietario diverso e senza permessi per l'utente da rimuovere.
     * Mocka il repository per trovare la nota.
     * Verifica che venga lanciata RuntimeException con messaggio di accesso negato.
     * Controlla che non vengano effettuati salvataggi.
     */
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

    /**
     * Test che verifica il fallimento della rimozione quando la nota
     * indicata per ID non viene trovata.
     * <p>
     * Mocka il repository per restituire Optional.empty() per l'ID specificato.
     * Verifica che venga lanciata RuntimeException con messaggio "Nota non trovata".
     * Assicura che non vengano fatti tentativi di salvataggio.
     */
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

    /**
     * Test che verifica l'aggiornamento della data di modifica di una nota
     * quando viene rimosso un utente dalla condivisione.
     * <p>
     * Prepara una nota con data modifica iniziale e un utente nei permessi di lettura.
     * Mocka il repository per trovare e salvare la nota.
     * Chiama il metodo di rimozione utente.
     * Verifica che la data di modifica sia successiva a quella originale.
     */
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

    /**
     * Test di aggiornamento di una nota da parte del proprietario.
     * <p>
     * Prepara una nota con tag e cartelle esistenti.
     * Prepara una richiesta di aggiornamento con nuovi dati e nuovi set di tag e cartelle.
     * Mocka il repository per trovare e salvare la nota.
     * Verifica che i dati della nota siano aggiornati correttamente,
     * che i tag e cartelle siano sostituiti,
     * e che la data di modifica sia aggiornata.
     * Controlla le chiamate al repository.
     */
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

    /**
     * Test di aggiornamento di una nota da parte di un utente con permessi di scrittura.
     * <p>
     * Prepara una nota con permessi scrittura che includono l'utente.
     * Prepara una richiesta di aggiornamento.
     * Mocka il repository per trovare e salvare la nota.
     * Verifica che i dati della nota siano aggiornati correttamente.
     * Controlla la chiamata al repository per il salvataggio.
     */

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

    /**
     * Test che verifica il fallimento dell'aggiornamento nota
     * quando l'utente non ha i permessi necessari.
     * <p>
     * Prepara una nota di un altro utente senza permessi per l'utente di test.
     * Prepara una richiesta di aggiornamento.
     * Mocka il repository per trovare la nota.
     * Verifica che venga lanciata RuntimeException con messaggio di permessi negati.
     * Assicura che non venga fatto alcun salvataggio.
     */
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

    /**
     * Test che verifica il fallimento dell'aggiornamento
     * quando la nota non viene trovata tramite ID.
     * <p>
     * Mocka il repository per restituire Optional.empty().
     * Verifica che venga lanciata RuntimeException con messaggio "Nota non trovata".
     * Assicura che non vengano fatti tentativi di salvataggio.
     */
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

    /**
     * Test che verifica la corretta gestione dei valori null per tags e cartelle
     * durante l'aggiornamento di una nota.
     * <p>
     * Prepara una nota con tags e cartelle inizialmente valorizzati.
     * Prepara una richiesta di aggiornamento con tags e cartelle null.
     * Mocka il repository per trovare e salvare la nota.
     * Verifica che i set tags e cartelle siano svuotati correttamente
     * e che la nota venga salvata senza errori.
     */

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

    /**
     * Verifica che durante l'aggiornamento della nota vengano modificati solo i campi cambiati,
     * lasciando invariati tag e cartelle, e aggiornando la data di modifica.
     */

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

    /**
     * Verifica che gli spazi bianchi in eccesso vengano rimossi dal titolo e dal contenuto
     * prima di salvare la nota aggiornata.
     */

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



    /**
     * Verifica che la cronologia delle versioni venga restituita correttamente
     * per una nota a cui l'utente ha accesso.
     */
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

    /**
     * Verifica che venga sollevata un'eccezione se si tenta di ottenere la cronologia
     * di una nota non esistente.
     */
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

    /**
     * Verifica che venga sollevata un'eccezione se l'utente non ha accesso alla nota
     * di cui si richiede la cronologia delle versioni.
     */

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

    /**
     * Verifica che venga restituita una versione specifica della nota se accessibile
     * all'utente che la richiede.
     */

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

    /**
     * Verifica che venga restituito Optional vuoto se la versione richiesta non esiste.
     */

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

    /**
     * Verifica che il ripristino di una versione precedente della nota aggiorni
     * correttamente titolo e contenuto.
     */

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
                note.getTitolo().equals("Titolo v2")
                        && note.getContenuto().equals("Contenuto v2")
        ));
        verify(noteVersionService).createVersion(any(Note.class), eq("testuser"), contains("Ripristino alla versione 2"));
    }

    /**
     * Verifica che venga sollevata un'eccezione se si tenta di ripristinare
     * una versione non esistente.
     */

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

    /**
     * Verifica che il ripristino di una versione fallisca se l'utente non ha
     * permessi di scrittura sulla nota.
     */

    @Test
    @DisplayName("Dovrebbe fallire il ripristino se utente non ha accesso di scrittura")
    void shouldFailRestoreIfUserHasNoWriteAccess() {
        // Given
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));


        assertThatThrownBy(() -> noteService.restoreNoteVersion(1L, 2, "altrouser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Non hai i permessi per ripristinare versioni di questa nota");

        verify(noteRepository).findById(1L);
        verify(noteVersionService, never()).getVersion(anyLong(), anyInt());
    }

    /**
     * Verifica il corretto confronto tra due versioni di una nota,
     * rilevando modifiche nel titolo e nel contenuto.
     */

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


    /**
     * Verifica che venga sollevata un'eccezione se si tenta di confrontare
     * versioni di nota non esistenti.
     */

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

    /**
     * Verifica che venga creata una nuova versione della nota durante l'aggiornamento
     * del contenuto o del titolo.
     */

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

    /**
     * Verifica che l'aggiornamento dei permessi della nota non comporti la creazione
     * di una nuova versione.
     */

    @Test
    @DisplayName("Dovrebbe aggiornare i permessi senza creare una versione")
    void shouldUpdatePermissionsWithoutCreatingVersion() {
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


        verify(noteVersionService, never()).createVersion(
                any(Note.class),
                anyString(),
                anyString()
        );
    }

    /**
     * Verifica che tutte le versioni di una nota vengano eliminate
     * quando la nota stessa viene cancellata.
     */

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

    /**
     * Test per la modifica di una nota da parte di un collaboratore con permessi di scrittura.
     * Verifica che la modifica venga accettata.
     */

    @Test
    @DisplayName("UC4.S13 - Test modifica nota con permessi scrittura")
    void testUpdateNoteWithWritePermission() {
        // Arrange
        User collaborator = new User("collaborator", "password123");
        collaborator.setId(2L);

        Note note = new Note("Test Note", "Test content", testUser);
        note.setId(1L);
        note.setPermessiScrittura(Set.of("collaborator"));
        note.setTipoPermesso(TipoPermesso.CONDIVISA_SCRITTURA);

        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitolo("Titolo Modificato");
        updateRequest.setContenuto("Contenuto modificato da collaboratore");
        updateRequest.setTags(Set.of("updated", "collaboration"));

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
      //  when(userRepository.findByUsername("collaborator")).thenReturn(Optional.of(collaborator));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        NoteDto result = noteService.updateNote(1L, updateRequest, "collaborator");

        // Assert
        assertNotNull(result);
        assertEquals("Titolo Modificato", result.getTitolo());
        assertEquals("Contenuto modificato da collaboratore", result.getContenuto());
        assertTrue(result.getTags().contains("updated"));
        assertTrue(result.getTags().contains("collaboration"));

        verify(noteRepository).save(any(Note.class));
    }

    /**
     * Test che verifica il rifiuto della modifica di una nota da parte
     * di un utente senza permessi di scrittura.
     */

    @Test
    @DisplayName("UC4.S14 - Test rifiuto modifica senza permessi")
    void testRejectUpdateWithoutPermission() {
        // Arrange
        Note note = new Note("Private Note", "Private content", testUser);
        note.setId(1L);
        note.setTipoPermesso(TipoPermesso.PRIVATA);
        note.setPermessiScrittura(new HashSet<>());

        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitolo("Tentativo Modifica");
        updateRequest.setContenuto("Contenuto non autorizzato");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        // Act & Assert
        assertThatThrownBy(() -> noteService.updateNote(1L, updateRequest, "unauthorized"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Non hai i permessi per modificare questa nota");

        verify(noteRepository, never()).save(any());
    }

    /**
     * Test che conferma che il proprietario della nota può sempre modificarla.
     */

    @Test
    @DisplayName("UC4.S15 - Test modifica nota da proprietario sempre consentita")
    void testOwnerCanAlwaysUpdate() {
        // Arrange
        Note note = new Note("Owner Note", "Owner content", testUser);
        note.setId(1L);
        note.setTipoPermesso(TipoPermesso.PRIVATA);

        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitolo("Modifica Proprietario");
        updateRequest.setContenuto("Il proprietario può sempre modificare");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        NoteDto result = noteService.updateNote(1L, updateRequest, "testuser");

        // Assert
        assertNotNull(result);
        verify(noteRepository).save(any());
    }

    /**
     * Test che verifica il fallimento della modifica di una nota
     * da parte di un utente con permessi solo di lettura.
     */

    @Test
    @DisplayName("UC4.S16 - Test modifica nota con permessi solo lettura fallisce")
    void testUpdateNoteWithReadOnlyPermission() {
        // Arrange
        Note note = new Note("Shared Note", "Shared content", testUser);
        note.setId(1L);
        note.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA);
        note.setPermessiLettura(Set.of("reader"));
        note.setPermessiScrittura(new HashSet<>());

        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitolo("Tentativo Modifica");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        // Act & Assert
        assertThatThrownBy(() -> noteService.updateNote(1L, updateRequest, "reader"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Non hai i permessi per modificare questa nota");

        verify(noteRepository, never()).save(any());
    }

    /**
     * Test per l'aggiornamento parziale di una nota, verificando
     * che i campi specificati vengano aggiornati correttamente.
     */

    @Test
    @DisplayName("UC4.S17 - Test aggiornamento campi specifici")
    void testPartialUpdate() {
        // Arrange
        Note note = new Note("Original Title", "Original content", testUser);
        note.setId(1L);
        note.setTags(Set.of("original-tag"));
        note.setCartelle(Set.of("original-folder"));

        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitolo("Solo Nuovo Titolo");
        updateRequest.setContenuto("");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        NoteDto result = noteService.updateNote(1L, updateRequest, "testuser");

        // Assert
        assertNotNull(result);
        assertEquals("Solo Nuovo Titolo", result.getTitolo());

        verify(noteRepository).save(argThat(savedNote ->
                savedNote.getTitolo().equals("Solo Nuovo Titolo")
        ));
    }

    /**
     * Test per la gestione dei permessi condivisi in modalità scrittura,
     * verificando che utenti autorizzati possano modificare la nota.
     */

    @Test
    @DisplayName("UC4.S18 - Test gestione permessi condivisa scrittura")
    void testSharedWritePermissions() {
        // Arrange
        User owner = new User("owner", "pass123");
        owner.setId(1L);

        Note sharedNote = new Note("Shared Note", "Shared content", owner);
        sharedNote.setId(1L);
        sharedNote.setTipoPermesso(TipoPermesso.CONDIVISA_SCRITTURA);
        sharedNote.setPermessiScrittura(Set.of("writer1", "writer2"));

        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitolo("Modified by Writer");
        updateRequest.setContenuto("Content modified by authorized writer");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(sharedNote));
        when(noteRepository.save(any(Note.class))).thenReturn(sharedNote);

        // Act
        NoteDto result = noteService.updateNote(1L, updateRequest, "writer1");

        // Assert
        assertNotNull(result);
        assertEquals("Modified by Writer", result.getTitolo());
        verify(noteRepository).save(any());
    }

    /**
     * Test che verifica la conservazione dei permessi di lettura e scrittura
     * durante l'aggiornamento della nota.
     */

    @Test
    @DisplayName("UC4.S19 - Test conservazione permessi durante modifica")
    void testPermissionsPreservedDuringUpdate() {
        // Arrange
        Note note = new Note("Shared Note", "Content", testUser);
        note.setId(1L);
        note.setTipoPermesso(TipoPermesso.CONDIVISA_SCRITTURA);
        note.setPermessiLettura(Set.of("reader1", "reader2"));
        note.setPermessiScrittura(Set.of("writer1"));

        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitolo("Updated Title");
        updateRequest.setContenuto("Updated content");

        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenReturn(note);

        // Act
        noteService.updateNote(1L, updateRequest, "testuser");

        // Assert
        verify(noteRepository).save(argThat(savedNote ->
                savedNote.getPermessiLettura().contains("reader1")
                        && savedNote.getPermessiLettura().contains("reader2")
                        && savedNote.getPermessiScrittura().contains("writer1")
                        && savedNote.getTipoPermesso() == TipoPermesso.CONDIVISA_SCRITTURA
        ));
    }


}