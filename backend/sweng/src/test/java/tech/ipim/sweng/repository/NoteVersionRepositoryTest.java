package tech.ipim.sweng.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.NoteVersion;
import tech.ipim.sweng.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NoteVersionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NoteVersionRepository noteVersionRepository;

    private User testUser;
    private Note testNote;
    private NoteVersion version1;
    private NoteVersion version2;
    private NoteVersion version3;

    @BeforeEach
    void setUp() {
        // Crea utente di test
        testUser = new User("testuser", "password123");
        testUser.setEmail("test@example.com");
        testUser = entityManager.persistAndFlush(testUser);

        // Crea nota di test
        testNote = new Note("Titolo Originale", "Contenuto Originale", testUser);
        testNote = entityManager.persistAndFlush(testNote);

        // Crea versioni di test
        version1 = new NoteVersion(testNote, 1, "Contenuto v1", "Titolo v1", "testuser", "Prima versione");
        version1.setCreatedAt(LocalDateTime.now().minusHours(3));
        version1 = entityManager.persistAndFlush(version1);

        version2 = new NoteVersion(testNote, 2, "Contenuto v2", "Titolo v2", "testuser", "Seconda versione");
        version2.setCreatedAt(LocalDateTime.now().minusHours(2));
        version2 = entityManager.persistAndFlush(version2);

        version3 = new NoteVersion(testNote, 3, "Contenuto v3", "Titolo v3", "altrouser", "Terza versione");
        version3.setCreatedAt(LocalDateTime.now().minusHours(1));
        version3 = entityManager.persistAndFlush(version3);
    }

    @Test
    @DisplayName("Dovrebbe trovare versioni ordinate per numero decrescente")
    void shouldFindVersionsByNoteIdOrderedByVersionNumberDesc() {
        // When
        List<NoteVersion> versions = noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(testNote.getId());

        // Then
        assertThat(versions).hasSize(3);
        assertThat(versions.get(0).getVersionNumber()).isEqualTo(3);
        assertThat(versions.get(1).getVersionNumber()).isEqualTo(2);
        assertThat(versions.get(2).getVersionNumber()).isEqualTo(1);

        // Verifica che tutte appartengano alla stessa nota
        assertThat(versions).allMatch(v -> v.getNote().getId().equals(testNote.getId()));
    }

    @Test
    @DisplayName("Dovrebbe trovare il numero di versione più alto")
    void shouldFindLatestVersionNumber() {
        // When
        Optional<Integer> latestVersionNumber = noteVersionRepository.findLatestVersionNumber(testNote.getId());

        // Then
        assertThat(latestVersionNumber).isPresent();
        assertThat(latestVersionNumber.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Dovrebbe restituire vuoto se non ci sono versioni")
    void shouldReturnEmptyIfNoVersionsExist() {
        // Given - Crea una nuova nota senza versioni
        Note noteWithoutVersions = new Note("Nota Nuova", "Contenuto Nuovo", testUser);
        noteWithoutVersions = entityManager.persistAndFlush(noteWithoutVersions);

        // When
        Optional<Integer> latestVersionNumber = noteVersionRepository.findLatestVersionNumber(noteWithoutVersions.getId());
        List<NoteVersion> versions = noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteWithoutVersions.getId());

        // Then
        assertThat(latestVersionNumber).isEmpty();
        assertThat(versions).isEmpty();
    }

    @Test
    @DisplayName("Dovrebbe trovare una versione specifica per nota e numero")
    void shouldFindVersionByNoteIdAndVersionNumber() {
        // When
        Optional<NoteVersion> foundVersion = noteVersionRepository.findByNoteIdAndVersionNumber(testNote.getId(), 2);

        // Then
        assertThat(foundVersion).isPresent();
        assertThat(foundVersion.get().getVersionNumber()).isEqualTo(2);
        assertThat(foundVersion.get().getContenuto()).isEqualTo("Contenuto v2");
        assertThat(foundVersion.get().getTitolo()).isEqualTo("Titolo v2");
        assertThat(foundVersion.get().getChangeDescription()).isEqualTo("Seconda versione");
    }

    @Test
    @DisplayName("Dovrebbe restituire vuoto per versione inesistente")
    void shouldReturnEmptyForNonExistentVersion() {
        // When
        Optional<NoteVersion> foundVersion = noteVersionRepository.findByNoteIdAndVersionNumber(testNote.getId(), 999);

        // Then
        assertThat(foundVersion).isEmpty();
    }

    @Test
    @DisplayName("Dovrebbe restituire vuoto per nota inesistente")
    void shouldReturnEmptyForNonExistentNote() {
        // When
        Optional<NoteVersion> foundVersion = noteVersionRepository.findByNoteIdAndVersionNumber(999L, 1);

        // Then
        assertThat(foundVersion).isEmpty();
    }

    @Test
    @DisplayName("Dovrebbe trovare la cronologia delle versioni ordinata per numero decrescente")
    void shouldFindVersionHistoryOrdered() {
        // When
        List<NoteVersion> history = noteVersionRepository.findVersionHistory(testNote.getId());

        // Then
        assertThat(history).hasSize(3);

        // Verifica ordinamento per numero di versione decrescente
        assertThat(history.get(0).getVersionNumber()).isEqualTo(3);
        assertThat(history.get(1).getVersionNumber()).isEqualTo(2);
        assertThat(history.get(2).getVersionNumber()).isEqualTo(1);

        // Verifica contenuti
        assertThat(history.get(0).getCreatedBy()).isEqualTo("altrouser");
        assertThat(history.get(1).getCreatedBy()).isEqualTo("testuser");
        assertThat(history.get(2).getCreatedBy()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Dovrebbe restituire lista vuota per nota senza versioni")
    void shouldReturnEmptyListForNoteWithoutVersions() {
        // Given
        Note noteWithoutVersions = new Note("Nota Senza Versioni", "Contenuto", testUser);
        noteWithoutVersions = entityManager.persistAndFlush(noteWithoutVersions);

        // When
        List<NoteVersion> history = noteVersionRepository.findVersionHistory(noteWithoutVersions.getId());

        // Then
        assertThat(history).isEmpty();
    }

    @Test
    @DisplayName("Dovrebbe salvare una nuova versione correttamente")
    void shouldSaveNewVersionCorrectly() {
        // Given
        NoteVersion newVersion = new NoteVersion(testNote, 4, "Nuovo contenuto", "Nuovo titolo", "nuovoutente", "Quarta versione");

        // When
        NoteVersion savedVersion = noteVersionRepository.save(newVersion);

        // Then
        assertThat(savedVersion).isNotNull();
        assertThat(savedVersion.getId()).isNotNull();
        assertThat(savedVersion.getVersionNumber()).isEqualTo(4);
        assertThat(savedVersion.getContenuto()).isEqualTo("Nuovo contenuto");
        assertThat(savedVersion.getTitolo()).isEqualTo("Nuovo titolo");
        assertThat(savedVersion.getCreatedBy()).isEqualTo("nuovoutente");
        assertThat(savedVersion.getChangeDescription()).isEqualTo("Quarta versione");
        assertThat(savedVersion.getCreatedAt()).isNotNull();

        // Verifica che sia stato salvato nel database
        Optional<NoteVersion> foundVersion = noteVersionRepository.findByNoteIdAndVersionNumber(testNote.getId(), 4);
        assertThat(foundVersion).isPresent();
        assertThat(foundVersion.get().getId()).isEqualTo(savedVersion.getId());
    }

    @Test
    @DisplayName("Dovrebbe eliminare una versione correttamente")
    void shouldDeleteVersionCorrectly() {
        // Given
        Long versionId = version2.getId();

        // When
        noteVersionRepository.delete(version2);
        entityManager.flush();

        // Then
        Optional<NoteVersion> deletedVersion = noteVersionRepository.findById(versionId);
        assertThat(deletedVersion).isEmpty();

        // Verifica che le altre versioni esistano ancora
        List<NoteVersion> remainingVersions = noteVersionRepository.findVersionHistory(testNote.getId());
        assertThat(remainingVersions).hasSize(2);
        assertThat(remainingVersions).extracting(NoteVersion::getVersionNumber)
                .containsExactly(3, 1);
    }

    @Test
    @DisplayName("Dovrebbe gestire relazione con nota correttamente")
    void shouldHandleNoteRelationshipCorrectly() {
        // Given
        User anotherUser = new User("anotheruser", "password");
        anotherUser.setEmail("another@example.com");
        anotherUser = entityManager.persistAndFlush(anotherUser);

        Note anotherNote = new Note("Altra Nota", "Altro Contenuto", anotherUser);
        anotherNote = entityManager.persistAndFlush(anotherNote);

        NoteVersion versionForAnotherNote = new NoteVersion(anotherNote, 1, "Contenuto altra nota", "Titolo altra nota", "anotheruser", "Prima versione altra nota");
        versionForAnotherNote = entityManager.persistAndFlush(versionForAnotherNote);

        // When
        List<NoteVersion> versionsForTestNote = noteVersionRepository.findVersionHistory(testNote.getId());
        List<NoteVersion> versionsForAnotherNote = noteVersionRepository.findVersionHistory(anotherNote.getId());

        // Then
        assertThat(versionsForTestNote).hasSize(3);
        assertThat(versionsForAnotherNote).hasSize(1);

        // Verifica che le versioni appartengano alle note corrette
        assertThat(versionsForTestNote).allMatch(v -> v.getNote().getId().equals(testNote.getId()));
       // assertThat(versionsForAnotherNote).allMatch(v -> v.getNote().getId().equals(anotherNote.getId()));
    }

    @Test
    @DisplayName("Dovrebbe gestire caratteri speciali nel contenuto")
    void shouldHandleSpecialCharactersInContent() {
        // Given
        String specialContent = "Contenuto con caratteri speciali: àèìòù ñ çë €@#$%^&*()[]{}|\\:;\"'<>?/";
        String specialTitle = "Titolo con émoji 🚀 e àccenti";

        NoteVersion specialVersion = new NoteVersion(testNote, 4, specialContent, specialTitle, "testuser", "Versione con caratteri speciali");

        // When
        NoteVersion savedVersion = noteVersionRepository.save(specialVersion);
        entityManager.flush();

        Optional<NoteVersion> foundVersion = noteVersionRepository.findByNoteIdAndVersionNumber(testNote.getId(), 4);

        // Then
        assertThat(foundVersion).isPresent();
        assertThat(foundVersion.get().getContenuto()).isEqualTo(specialContent);
        assertThat(foundVersion.get().getTitolo()).isEqualTo(specialTitle);
    }
}