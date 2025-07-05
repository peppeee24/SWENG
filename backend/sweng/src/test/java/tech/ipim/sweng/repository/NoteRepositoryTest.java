package tech.ipim.sweng.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.model.TipoPermesso;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;


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
        sharedNote.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA); // CORREZIONE: Uso corretto dell'enum
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
        // When
        List<Note> sharedNotes = noteRepository.findAllAccessibleNotes("testuser2"); // CORREZIONE: Metodo corretto

        // Then
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



    @Test
    @DisplayName("REPO-AUTHOR-001: Dovrebbe trovare note per autore specifico accessibili")
    void shouldFindNotesByAuthorAccessible() {
        // Given - crea nota di testUser2 condivisa con testUser1
        Note notaCondivisaMario = new Note("Nota di Mario", "Contenuto di Mario", testUser2);
        notaCondivisaMario.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA);
        notaCondivisaMario.setPermessiLettura(Set.of("testuser1"));
        entityManager.persistAndFlush(notaCondivisaMario);

        // When
        List<Note> result = noteRepository.findByAutoreUsernameAndAccessibleByUser("testuser2", "testuser1");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).isEqualTo("Nota di Mario");
        assertThat(result.get(0).getAutore().getUsername()).isEqualTo("testuser2");
    }

    @Test
    @DisplayName("REPO-AUTHOR-002: Dovrebbe escludere note private non accessibili per autore")
    void shouldExcludePrivateNotesFromAuthorSearch() {
        // Given - crea nota privata di testUser2 (non condivisa)
        Note notaPrivataMario = new Note("Nota Privata Mario", "Contenuto privato", testUser2);
        notaPrivataMario.setTipoPermesso(TipoPermesso.PRIVATA);
        entityManager.persistAndFlush(notaPrivataMario);

        // When - testUser1 cerca note di testUser2
        List<Note> result = noteRepository.findByAutoreUsernameAndAccessibleByUser("testuser2", "testuser1");

        // Then - non dovrebbe trovare la nota privata
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("REPO-AUTHOR-003: Dovrebbe essere case-sensitive per username autore")
    void shouldBeCaseSensitiveForAuthorSearch() {
        // When
        List<Note> resultLower = noteRepository.findByAutoreUsernameAndAccessibleByUser("testuser1", "testuser1");
        List<Note> resultUpper = noteRepository.findByAutoreUsernameAndAccessibleByUser("TESTUSER1", "testuser1");

        // Then
        assertThat(resultLower).hasSize(2); // Le note esistenti di testUser1
        assertThat(resultUpper).isEmpty(); // Username maiuscolo non esiste
    }



    @Test
    @DisplayName("REPO-DATE-001: Dovrebbe trovare note in range di date specifico")
    void shouldFindNotesInDateRange() {
        // Given - modifica date delle note esistenti
        privateNote.setDataModifica(LocalDateTime.now().minusDays(5));
        sharedNote.setDataModifica(LocalDateTime.now().minusDays(2));
        entityManager.merge(privateNote);
        entityManager.merge(sharedNote);
        entityManager.flush();

        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now().minusDays(1);

        // When
        List<Note> result = noteRepository.findByDateRangeAndAccessibleByUser(startDate, endDate, "testuser1");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Note::getTitolo)
                .containsExactlyInAnyOrder("Nota Privata", "Nota Condivisa");
    }

    @Test
    @DisplayName("REPO-DATE-002: Dovrebbe ordinare risultati per data modifica decrescente")
    void shouldOrderByModificationDateDesc() {
        // Given - crea note con date specifiche
        Note notaVecchia = new Note("Nota Vecchia", "Contenuto vecchio", testUser1);
        notaVecchia.setDataModifica(LocalDateTime.now().minusDays(10));

        Note notaRecente = new Note("Nota Recente", "Contenuto recente", testUser1);
        notaRecente.setDataModifica(LocalDateTime.now().minusHours(1));

        entityManager.persist(notaVecchia);
        entityManager.persist(notaRecente);
        entityManager.flush();

        LocalDateTime startDate = LocalDateTime.now().minusDays(15);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        List<Note> result = noteRepository.findByDateRangeAndAccessibleByUser(startDate, endDate, "testuser1");

        // Then - verifica ordinamento (più recente prima)
        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getDataModifica())
                    .isAfterOrEqualTo(result.get(i + 1).getDataModifica());
        }
    }

    @Test
    @DisplayName("REPO-DATE-003: Dovrebbe gestire range vuoto senza errori")
    void shouldHandleEmptyDateRange() {
        // Given - range futuro senza note
        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);

        // When
        List<Note> result = noteRepository.findByDateRangeAndAccessibleByUser(startDate, endDate, "testuser1");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("REPO-DATE-004: Dovrebbe rispettare permessi di accesso nel filtro data")
    void shouldRespectAccessPermissionsInDateFilter() {
        // Given - nota di testUser2 non condivisa, nel range di date
        Note notaNonAccessibile = new Note("Nota Non Accessibile", "Contenuto", testUser2);
        notaNonAccessibile.setDataModifica(LocalDateTime.now().minusHours(2));
        notaNonAccessibile.setTipoPermesso(TipoPermesso.PRIVATA);
        entityManager.persist(notaNonAccessibile);
        entityManager.flush();

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        // When - testUser1 cerca nel range
        List<Note> result = noteRepository.findByDateRangeAndAccessibleByUser(startDate, endDate, "testuser1");

        // Then - non dovrebbe vedere la nota privata di testUser2
        assertThat(result).noneMatch(note ->
                note.getTitolo().equals("Nota Non Accessibile"));
    }



    @Test
    @DisplayName("REPO-COMBINED-001: Dovrebbe combinare filtri autore e data")
    void shouldCombineAuthorAndDateFilters() {
        // Given - prepara note con autore e date specifiche
        Note notaMarioRecente = new Note("Nota Mario Recente", "Contenuto", testUser2);
        notaMarioRecente.setDataModifica(LocalDateTime.now().minusHours(2));
        notaMarioRecente.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA);
        notaMarioRecente.setPermessiLettura(Set.of("testuser1"));

        Note notaMarioVecchia = new Note("Nota Mario Vecchia", "Contenuto", testUser2);
        notaMarioVecchia.setDataModifica(LocalDateTime.now().minusDays(10));
        notaMarioVecchia.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA);
        notaMarioVecchia.setPermessiLettura(Set.of("testuser1"));

        entityManager.persist(notaMarioRecente);
        entityManager.persist(notaMarioVecchia);
        entityManager.flush();

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        List<Note> result = noteRepository.findByAuthorAndDateRangeAndAccessibleByUser(
                "testuser2", startDate, endDate, "testuser1");

        // Then - dovrebbe trovare solo la nota recente di Mario
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).isEqualTo("Nota Mario Recente");
    }

    @Test
    @DisplayName("REPO-COMBINED-002: Dovrebbe gestire filtri combinati senza risultati")
    void shouldHandleCombinedFiltersWithNoResults() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(7);

        // When - cerca autore esistente ma in range futuro
        List<Note> result = noteRepository.findByAuthorAndDateRangeAndAccessibleByUser(
                "testuser1", startDate, endDate, "testuser1");

        // Then
        assertThat(result).isEmpty();
    }



    @Test
    @DisplayName("REPO-PERF-001: Dovrebbe essere performante con molte note")
    void shouldPerformWellWithManyNotes() {
        // Given - crea molte note
        for (int i = 1; i <= 50; i++) {
            Note nota = new Note("Nota Performance " + i, "Contenuto " + i, testUser1);
            nota.setDataModifica(LocalDateTime.now().minusDays(i % 30));
            entityManager.persist(nota);
        }
        entityManager.flush();

        LocalDateTime startDate = LocalDateTime.now().minusDays(35);
        LocalDateTime endDate = LocalDateTime.now();

        // When - misura performance
        long startTime = System.currentTimeMillis();
        List<Note> result = noteRepository.findByDateRangeAndAccessibleByUser(startDate, endDate, "testuser1");
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(result).hasSizeGreaterThanOrEqualTo(50);
        assertThat(endTime - startTime).isLessThan(100); // Meno di 100ms
    }



    /**
     * Metodo helper per verificare se una nota è accessibile da un utente
     */
    private boolean isNoteAccessibleByUser(Note note, String username) {
        return note.getAutore().getUsername().equals(username) ||
                (note.getPermessiLettura() != null && note.getPermessiLettura().contains(username)) ||
                (note.getPermessiScrittura() != null && note.getPermessiScrittura().contains(username));
    }

    /**
     * Metodo helper per creare una nota con data specifica
     */
    private Note createNoteWithDate(String title, String content, User author, LocalDateTime date) {
        Note note = new Note(title, content, author);
        note.setDataModifica(date);
        return note;
    }
}