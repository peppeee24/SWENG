package tech.ipim.sweng.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void shouldFindNotesByAutore() {
        // When
        List<Note> notes = noteRepository.findByAutoreOrderByDataModificaDesc(testUser1);

        // Then
        assertThat(notes).hasSize(2);
        assertThat(notes).extracting(Note::getTitolo)
                .containsExactly("Nota Condivisa", "Nota Privata"); // Ordinato per data modifica desc
    }

    @Test
    void shouldFindAllAccessibleNotesForOwner() {
        // When
        List<Note> notes = noteRepository.findAllAccessibleNotes("testuser1");

        // Then
        assertThat(notes).hasSize(2);
        assertThat(notes).extracting(Note::getTitolo)
                .contains("Nota Privata", "Nota Condivisa");
    }

    @Test
    void shouldFindOnlySharedNotesForNonOwner() {
        // When
        List<Note> notes = noteRepository.findAllAccessibleNotes("testuser2");

        // Then
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitolo()).isEqualTo("Nota Condivisa");
    }

    @Test
    void shouldFindAccessibleNoteById() {
        // When - proprietario accede alla sua nota
        Optional<Note> foundNote = noteRepository.findAccessibleNoteById(privateNote.getId(), "testuser1");

        // Then
        assertThat(foundNote).isPresent();
        assertThat(foundNote.get().getTitolo()).isEqualTo("Nota Privata");
    }

    @Test
    void shouldNotFindPrivateNoteForNonOwner() {
        // When - utente non proprietario tenta di accedere a nota privata
        Optional<Note> foundNote = noteRepository.findAccessibleNoteById(privateNote.getId(), "testuser2");

        // Then
        assertThat(foundNote).isEmpty();
    }

    @Test
    void shouldFindSharedNoteForAuthorizedUser() {
        // When - utente autorizzato accede a nota condivisa
        Optional<Note> foundNote = noteRepository.findAccessibleNoteById(sharedNote.getId(), "testuser2");

        // Then
        assertThat(foundNote).isPresent();
        assertThat(foundNote.get().getTitolo()).isEqualTo("Nota Condivisa");
    }

    @Test
    void shouldSearchNotesByKeyword() {
        // When
        List<Note> notes = noteRepository.searchNotesByKeyword("testuser1", "privato");

        // Then
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitolo()).isEqualTo("Nota Privata");
    }

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

    @Test
    void shouldFindNotesByTag() {
        // When
        List<Note> notes = noteRepository.findNotesByTag("testuser1", "importante");

        // Then
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitolo()).isEqualTo("Nota Privata");
    }

    @Test
    void shouldFindNotesByCartella() {
        // When
        List<Note> notes = noteRepository.findNotesByCartella("testuser1", "Progetto A");

        // Then
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitolo()).isEqualTo("Nota Privata");
    }

    @Test
    void shouldCountNotesByAutore() {
        // When
        long count = noteRepository.countByAutore(testUser1);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCountSharedNotes() {
        // When
        long count = noteRepository.countSharedNotes("testuser2");

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldFindAllTagsByUser() {
        // When
        List<String> tags = noteRepository.findAllTagsByUser("testuser1");

        // Then
        assertThat(tags).containsExactlyInAnyOrder("condiviso", "importante", "lavoro", "team");
    }

    @Test
    void shouldFindAllCartelleByUser() {
        // When
        List<String> cartelle = noteRepository.findAllCartelleByUser("testuser1");

        // Then
        assertThat(cartelle).containsExactlyInAnyOrder("Progetto A", "Progetti Condivisi");
    }

    @Test
    void shouldFindSharedNotesForUser() {

        List<Note> sharedNotes = noteRepository.findAllAccessibleNotes("testUser2");


        assertThat(sharedNotes).hasSize(1);
        assertThat(sharedNotes.get(0).getTitolo()).isEqualTo("Nota Condivisa");
    }

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