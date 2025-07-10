package tech.ipim.sweng.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.model.TipoPermesso;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test di integrazione per il repository {@link NoteRepository}, responsabile
 * della gestione delle query sulle entità {@link Note}, inclusa la gestione
 * dei permessi di accesso, ricerca per parole chiave, filtri per tag, cartelle e
 * conteggi note condivise.
 * <p>
 * Verifica il comportamento delle query personalizzate definite a livello di repository.
 * <p>
 * <p>
 * Riepilogo dei test implementati:
 * <ul>
 *   <li>{@code shouldFindNotesByAutore} – Recupera le note di un autore, ordinate per data di modifica</li>
 *   <li>{@code shouldFindAllAccessibleNotesForOwner} – Recupera tutte le note accessibili dal proprietario</li>
 *   <li>{@code shouldFindOnlySharedNotesForNonOwner} – Verifica che un utente non proprietario veda solo le note condivise</li>
 *   <li>{@code shouldFindAccessibleNoteById} – Verifica il recupero di una nota per ID se accessibile dall’utente</li>
 *   <li>{@code shouldNotFindPrivateNoteForNonOwner} – Verifica che una nota privata non sia accessibile da altri utenti</li>
 *   <li>{@code shouldFindSharedNoteForAuthorizedUser} – Verifica accesso a nota condivisa per un utente autorizzato</li>
 *   <li>{@code shouldSearchNotesByKeyword} – Ricerca note per parola chiave nel titolo o contenuto</li>
 *   <li>{@code shouldSearchNotesInTitleAndContent} – Ricerca combinata nel titolo e nel contenuto</li>
 *   <li>{@code shouldFindNotesByTag} – Filtra le note per tag associati</li>
 *   <li>{@code shouldFindNotesByCartella} – Filtra le note per nome di cartella</li>
 *   <li>{@code shouldCountNotesByAutore} – Conta quante note appartengono a un autore</li>
 *   <li>{@code shouldCountSharedNotes} – Conta quante note condivise sono accessibili a un utente</li>
 *   <li>{@code shouldFindAllTagsByUser} – Recupera tutti i tag associati a note di un utente</li>
 *   <li>{@code shouldFindAllCartelleByUser} – Recupera tutte le cartelle associate a note di un utente</li>
 *   <li>{@code shouldFindSharedNotesForUser} – Recupera tutte le note condivise accessibili per un utente specifico</li>
 *   <li>{@code shouldNotFindNotesForUserWithoutAccess} – Verifica che un utente senza permessi non visualizzi alcuna nota</li>
 * </ul>
 */

@DataJpaTest
class NoteRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NoteRepository noteRepository;

    private User testUser1;
    private User testUser2;
    private Note privateNote;
    private Note sharedNote;

    /**
     * Configura dati di test: due utenti, una nota privata e una condivisa con permessi specifici.
     */

    @BeforeEach
    void setUp() {
        // Crea utenti di test
        testUser1 = new User("testuser1", "password123");
        testUser1.setEmail("test1@example.com");
        testUser1 = entityManager.persistAndFlush(testUser1);

        testUser2 = new User("testuser2", "password456");
        testUser2.setEmail("test2@example.com");
        testUser2 = entityManager.persistAndFlush(testUser2);

        // Crea nota privata
        privateNote = new Note("Nota Privata", "Contenuto privato di test", testUser1);
        privateNote.setTags(Set.of("lavoro", "importante"));
        privateNote.setCartelle(Set.of("Progetto A"));
        privateNote = entityManager.persistAndFlush(privateNote);

        // Crea nota condivisa
        sharedNote = new Note("Nota Condivisa", "Contenuto condiviso di test", testUser1);
        sharedNote.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA);
        sharedNote.setPermessiLettura(Set.of("testuser2"));
        sharedNote.setTags(Set.of("condiviso", "team"));
        sharedNote.setCartelle(Set.of("Progetti Condivisi"));
        sharedNote = entityManager.persistAndFlush(sharedNote);

        entityManager.clear();
    }

    /**
     * Verifica il recupero di tutte le note di un autore, ordinate per data di modifica decrescente.
     */

    @Test
    void shouldFindNotesByAutore() {
        // When
        List<Note> notes = noteRepository.findByAutoreOrderByDataModificaDesc(testUser1);

        // Then
        assertThat(notes).hasSize(2);
        assertThat(notes).extracting(Note::getTitolo)
                .containsExactly("Nota Condivisa", "Nota Privata"); // Ordinato per data modifica desc
    }

    /**
     * Verifica il recupero di tutte le note accessibili da un proprietario (private e condivise).
     */

    @Test
    void shouldFindAllAccessibleNotesForOwner() {
        // When
        List<Note> notes = noteRepository.findAllAccessibleNotes("testuser1");

        // Then
        assertThat(notes).hasSize(2);
        assertThat(notes).extracting(Note::getTitolo)
                .contains("Nota Privata", "Nota Condivisa");
    }

    /**
     * Verifica che un utente non proprietario possa vedere solo le note condivise.
     */

    @Test
    void shouldFindOnlySharedNotesForNonOwner() {
        // When
        List<Note> notes = noteRepository.findAllAccessibleNotes("testuser2");

        // Then
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitolo()).isEqualTo("Nota Condivisa");
    }

    /**
     * Verifica il recupero di una nota per ID se accessibile dal proprietario.
     */

    @Test
    void shouldFindAccessibleNoteById() {
        // When - proprietario accede alla sua nota
        Optional<Note> foundNote = noteRepository.findAccessibleNoteById(privateNote.getId(), "testuser1");

        // Then
        assertThat(foundNote).isPresent();
        assertThat(foundNote.get().getTitolo()).isEqualTo("Nota Privata");
    }

    /**
     * Verifica che una nota privata non sia accessibile da utenti non proprietari.
     */

    @Test
    void shouldNotFindPrivateNoteForNonOwner() {
        // When - utente non proprietario tenta di accedere a nota privata
        Optional<Note> foundNote = noteRepository.findAccessibleNoteById(privateNote.getId(), "testuser2");

        // Then
        assertThat(foundNote).isEmpty();
    }

    /**
     * Verifica che una nota condivisa sia accessibile da un utente autorizzato.
     */

    @Test
    void shouldFindSharedNoteForAuthorizedUser() {
        // When - utente autorizzato accede a nota condivisa
        Optional<Note> foundNote = noteRepository.findAccessibleNoteById(sharedNote.getId(), "testuser2");

        // Then
        assertThat(foundNote).isPresent();
        assertThat(foundNote.get().getTitolo()).isEqualTo("Nota Condivisa");
    }

    /**
     * Verifica la ricerca di note per parola chiave.
     */

    @Test
    void shouldSearchNotesByKeyword() {
        // When
        List<Note> notes = noteRepository.searchNotesByKeyword("testuser1", "privato");

        // Then
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitolo()).isEqualTo("Nota Privata");
    }

    /**
     * Verifica ricerca di note per keyword sia nel titolo che nel contenuto.
     */

    @Test
    void shouldSearchNotesInTitleAndContent() {
        // When - cerca nel titolo
        List<Note> notesByTitle = noteRepository.searchNotesByKeyword("testuser1", "Condivisa");

        // When - cerca nel contenuto
        List<Note> notesByContent = noteRepository.searchNotesByKeyword("testuser1", "condiviso");

        // Then
        assertThat(notesByTitle).hasSize(1);
        assertThat(notesByContent).hasSize(1);
    }

    /**
     * Verifica il recupero di note filtrate per tag.
     */

    @Test
    void shouldFindNotesByTag() {
        // When
        List<Note> notes = noteRepository.findNotesByTag("testuser1", "importante");

        // Then
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitolo()).isEqualTo("Nota Privata");
    }

    /**
     * Verifica il recupero di note filtrate per cartella.
     */

    @Test
    void shouldFindNotesByCartella() {
        // When
        List<Note> notes = noteRepository.findNotesByCartella("testuser1", "Progetto A");

        // Then
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitolo()).isEqualTo("Nota Privata");
    }

     /**
     * Conta quante note appartengono a un autore.
     */

    @Test
    void shouldCountNotesByAutore() {
        // When
        long count = noteRepository.countByAutore(testUser1);

        // Then
        assertThat(count).isEqualTo(2);
    }

    /**
     * Conta quante note condivise risultano accessibili a un determinato utente.
     */

    @Test
    void shouldCountSharedNotes() {
        // When
        long count = noteRepository.countSharedNotes("testuser2");

        // Then
        assertThat(count).isEqualTo(1);
    }

    /**
     * Recupera tutti i tag associati alle note di un utente.
     */

    @Test
    void shouldFindAllTagsByUser() {
        // When
        List<String> tags = noteRepository.findAllTagsByUser("testuser1");

        // Then
        assertThat(tags).containsExactlyInAnyOrder("condiviso", "importante", "lavoro", "team");
    }

    /**
     * Recupera tutte le cartelle associate alle note di un utente.
     */

    @Test
    void shouldFindAllCartelleByUser() {
        // When
        List<String> cartelle = noteRepository.findAllCartelleByUser("testuser1");

        // Then
        assertThat(cartelle).containsExactlyInAnyOrder("Progetto A", "Progetti Condivisi");
    }

    /**
     * Recupera tutte le note condivise accessibili a un utente.
     */

    @Test
    void shouldFindSharedNotesForUser() {
        // When
        List<Note> sharedNotes = noteRepository.findAllAccessibleNotes("testuser2");

        // Then
        assertThat(sharedNotes).hasSize(1);
        assertThat(sharedNotes.get(0).getTitolo()).isEqualTo("Nota Condivisa");
    }

    /**
     * Verifica che un utente senza permessi non visualizzi alcuna nota.
     */
    
    @Test
    void shouldNotFindNotesForUserWithoutAccess() {
        // Given - crea un terzo utente senza accesso
        User testUser3 = new User("testuser3", "password789");
        testUser3 = entityManager.persistAndFlush(testUser3);

        // When
        List<Note> notes = noteRepository.findAllAccessibleNotes("testuser3");

        // Then
        assertThat(notes).isEmpty();
    }
}