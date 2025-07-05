package tech.ipim.sweng.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class NoteRepositorySearchTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NoteRepository noteRepository;

    private User testUser;
    private User otherUser;
    private Note note1;
    private Note note2;
    private Note note3;
    private Note sharedNote;

    @BeforeEach
    void setUp() {
        // Crea utenti di test
        testUser = new User("testuser", "password123");
        otherUser = new User("otheruser", "password456");
        
        entityManager.persistAndFlush(testUser);
        entityManager.persistAndFlush(otherUser);

        // Crea note di test
        note1 = new Note("Prima Nota Java", "Contenuto sulla programmazione Java", testUser);
        note1.setCartelle(Set.of("Programmazione"));
        note1.setDataCreazione(LocalDateTime.of(2024, 1, 15, 10, 0));
        note1.setDataModifica(LocalDateTime.of(2024, 1, 16, 12, 0));
        note1.setTags(Set.of("importante", "lavoro"));

        note2 = new Note("Seconda Nota Python", "Contenuto sulla programmazione Python", testUser);
        note2.setCartelle(Set.of("Programmazione"));
        note2.setDataCreazione(LocalDateTime.of(2024, 2, 10, 14, 0));
        note2.setDataModifica(LocalDateTime.of(2024, 2, 11, 16, 0));
        note2.setTags(Set.of("importante"));

        note3 = new Note("Terza Nota Personale", "Contenuto personale privato", testUser);
        note3.setCartelle(Set.of("Personale"));
        note3.setDataCreazione(LocalDateTime.of(2024, 3, 5, 9, 0));
        note3.setDataModifica(LocalDateTime.of(2024, 3, 6, 11, 0));
        note3.setTags(Set.of("personale"));

        // Nota condivisa
        sharedNote = new Note("Nota Condivisa", "Contenuto condiviso con testuser", otherUser);
        sharedNote.setCartelle(Set.of("Condivisa"));
        sharedNote.setTipoPermesso(TipoPermesso.CONDIVISA_LETTURA);
        sharedNote.setPermessiLettura(Set.of("testuser"));
        sharedNote.setDataCreazione(LocalDateTime.of(2024, 1, 20, 11, 0));
        sharedNote.setDataModifica(LocalDateTime.of(2024, 1, 21, 13, 0));
        sharedNote.setTags(Set.of("condivisa"));

        entityManager.persistAndFlush(note1);
        entityManager.persistAndFlush(note2);
        entityManager.persistAndFlush(note3);
        entityManager.persistAndFlush(sharedNote);
    }

    @Test
    void shouldSearchNotesByKeywordInTitle() {
        // When
        List<Note> result = noteRepository.searchNotesByKeyword("testuser", "Java");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).contains("Java");
        assertThat(result.get(0).getAutore().getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldSearchNotesByKeywordInContent() {
        // When
        List<Note> result = noteRepository.searchNotesByKeyword("testuser", "programmazione");

        // Then
        assertThat(result).hasSize(2); // note1 e note2 contengono "programmazione"
        assertThat(result).allMatch(note -> 
            note.getContenuto().toLowerCase().contains("programmazione") ||
            note.getTitolo().toLowerCase().contains("programmazione"));
    }

    @Test
    void shouldSearchNotesCaseInsensitive() {
        // When
        List<Note> result = noteRepository.searchNotesByKeyword("testuser", "JAVA");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).containsIgnoringCase("java");
    }

    @Test
    void shouldReturnEmptyListWhenKeywordNotFound() {
        // When
        List<Note> result = noteRepository.searchNotesByKeyword("testuser", "inesistente");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindNotesByTag() {
        // When
        List<Note> result = noteRepository.findNotesByTag("testuser", "importante");

        // Then
        assertThat(result).hasSize(2); // note1 e note2 hanno il tag "importante"
        assertThat(result).allMatch(note -> note.getTags().contains("importante"));
        assertThat(result).allMatch(note -> note.getAutore().getUsername().equals("testuser"));
    }

    @Test
    void shouldFindNotesByCartella() {
        // When
        List<Note> result = noteRepository.findNotesByCartella("testuser", "Programmazione");

        // Then
        assertThat(result).hasSize(2); // note1 e note2 sono in "Programmazione"
        assertThat(result).allMatch(note -> note.getCartelle().contains("Programmazione"));
        assertThat(result).allMatch(note -> note.getAutore().getUsername().equals("testuser"));
    }

    @Test
    void shouldReturnEmptyListWhenCartellaNotFound() {
        // When
        List<Note> result = noteRepository.findNotesByCartella("testuser", "Inesistente");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindAllAccessibleNotes() {
        // When
        List<Note> result = noteRepository.findAllAccessibleNotes("testuser");

        // Then
        assertThat(result).hasSize(4); // 3 note proprie + 1 condivisa
        assertThat(result).anyMatch(note -> note.getAutore().getUsername().equals("testuser"));
        assertThat(result).anyMatch(note -> note.getAutore().getUsername().equals("otheruser") && 
                                           note.getTipoPermesso() == TipoPermesso.CONDIVISA_LETTURA);
    }

    @Test
    void shouldFindAccessibleNoteById() {
        // When - nota propria
        Optional<Note> ownNote = noteRepository.findAccessibleNoteById(note1.getId(), "testuser");
        
        // When - nota condivisa
        Optional<Note> sharedNoteResult = noteRepository.findAccessibleNoteById(sharedNote.getId(), "testuser");

        // Then
        assertThat(ownNote).isPresent();
        assertThat(ownNote.get().getId()).isEqualTo(note1.getId());
        
        assertThat(sharedNoteResult).isPresent();
        assertThat(sharedNoteResult.get().getId()).isEqualTo(sharedNote.getId());
    }

    @Test
    void shouldNotFindInaccessibleNoteById() {
        // Given - crea una nota privata di otherUser
        Note privateNote = new Note("Nota Privata", "Contenuto privato", otherUser);
        privateNote.setTipoPermesso(TipoPermesso.PRIVATA);
        entityManager.persistAndFlush(privateNote);

        // When
        Optional<Note> result = noteRepository.findAccessibleNoteById(privateNote.getId(), "testuser");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindNotesByAutoreOrderedByModificationDate() {
        // When
        List<Note> result = noteRepository.findByAutoreOrderByDataModificaDesc(testUser);

        // Then
        assertThat(result).hasSize(3);
        // Verifica ordine decrescente per data modifica
        assertThat(result.get(0).getDataModifica()).isAfter(result.get(1).getDataModifica());
        assertThat(result.get(1).getDataModifica()).isAfter(result.get(2).getDataModifica());
        assertThat(result).allMatch(note -> note.getAutore().equals(testUser));
    }

    @Test
    void shouldFindSharedNotesByAutore() {
        // When
        List<Note> result = noteRepository.findSharedNotesByAutore("testuser", "otheruser");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAutore().getUsername()).isEqualTo("otheruser");
        assertThat(result.get(0).getTipoPermesso()).isEqualTo(TipoPermesso.CONDIVISA_LETTURA);
        assertThat(result.get(0).getPermessiLettura()).contains("testuser");
    }

    @Test
    void shouldFindAccessibleNotesByAutore() {
        // When
        List<Note> result = noteRepository.findAccessibleNotesByAutore("testuser", "otheruser");

        // Then
        assertThat(result).hasSize(1); // Solo la nota condivisa di otheruser
        assertThat(result.get(0).getAutore().getUsername()).isEqualTo("otheruser");
    }

    @Test
    void shouldSearchIncludeSharedNotesInKeywordSearch() {
        // When
        List<Note> result = noteRepository.searchNotesByKeyword("testuser", "condiviso");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAutore().getUsername()).isEqualTo("otheruser");
        assertThat(result.get(0).getContenuto()).contains("condiviso");
    }

    @Test
    void shouldFindTagsInSharedNotes() {
        // When
        List<Note> result = noteRepository.findNotesByTag("testuser", "condivisa");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAutore().getUsername()).isEqualTo("otheruser");
        assertThat(result.get(0).getTags()).contains("condivisa");
    }

    @Test
    void shouldExcludeOtherUsersPrivateNotesFromSearch() {
        // Given - crea una nota privata di otherUser
        Note privateNote = new Note("Nota Java Privata", "Contenuto Java privato", otherUser);
        privateNote.setTipoPermesso(TipoPermesso.PRIVATA);
        entityManager.persistAndFlush(privateNote);

        // When
        List<Note> result = noteRepository.searchNotesByKeyword("testuser", "Java");

        // Then - dovrebbe trovare solo la nota di testuser, non quella privata di otheruser
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAutore().getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldHandlePartialKeywordMatch() {
        // When
        List<Note> result = noteRepository.searchNotesByKeyword("testuser", "Prim");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).contains("Prima");
    }

    @Test
    void shouldFindDistinctAutoriByAccessibleToUser() {
        // When
        List<String> result = noteRepository.findDistinctAutoriByAccessibleToUser("testuser");

        // Then
        assertThat(result).contains("testuser", "otheruser");
        assertThat(result).hasSize(2);
    }

    @Test
    void shouldCountNotesByAutore() {
        // When
        long count = noteRepository.countNotesByAutore("testuser");

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void shouldCountSharedNotesForUser() {
        // When
        long count = noteRepository.countSharedNotesForUser("testuser");

        // Then
        assertThat(count).isEqualTo(1); // Solo sharedNote
    }

    @Test
    void shouldFindAllTagsByUser() {
        // When
        List<String> result = noteRepository.findAllTagsByUser("testuser");

        // Then
        assertThat(result).contains("importante", "lavoro", "personale", "condivisa");
        assertThat(result).hasSize(4);
    }

    @Test
    void shouldFindAllCartelleByUser() {
        // When
        List<String> result = noteRepository.findAllCartelleByUser("testuser");

        // Then
        assertThat(result).contains("Programmazione", "Personale");
        assertThat(result).hasSize(2); // Solo le cartelle delle note proprie
    }

    @Test
    void shouldSearchNotesWithCaseInsensitiveKeyword() {
        // When
        List<Note> result = noteRepository.searchNotesByKeyword("testuser", "JAVA");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).containsIgnoringCase("java");
    }

    @Test
    void shouldSearchNotesWithPartialKeyword() {
        // When
        List<Note> result = noteRepository.searchNotesByKeyword("testuser", "Prim");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitolo()).contains("Prima");
    }

    @Test
    void shouldExcludeOtherUsersNotesFromSearch() {
        // When - otherUser cerca "programmazione"
        List<Note> result = noteRepository.searchNotesByKeyword("otheruser", "programmazione");

        // Then - dovrebbe trovare solo la sua nota condivisa (se accessibile)
        assertThat(result).hasSize(0); // otheruser non ha note proprie con "programmazione"
    }
}