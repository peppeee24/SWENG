package tech.ipim.sweng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.NoteVersion;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteVersionRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoteVersionService - Test del sistema di versionamento")
class NoteVersionServiceTest {

    @Mock
    private NoteVersionRepository noteVersionRepository;

    @InjectMocks
    private NoteVersionService noteVersionService;

    private User testUser;
    private Note testNote;
    private NoteVersion testVersion;

    @BeforeEach
    void setUp() {
        testUser = createTestUser("testuser", "test@example.com");
        testNote = createTestNote(testUser);
        testVersion = new NoteVersion(testNote, 1, "Contenuto v1", "Titolo v1", "testuser", "Prima versione");
    }

    // ===== TEST CREAZIONE VERSIONI =====

    @Test
    @DisplayName("VER-001: Dovrebbe creare una nuova versione")
    void shouldCreateNewVersion() {
        // Given
        when(noteVersionRepository.findMaxVersionNumber(1L)).thenReturn(Optional.of(1));
        when(noteVersionRepository.save(any(NoteVersion.class))).thenReturn(testVersion);

        // When
        NoteVersion result = noteVersionService.createVersion(testNote, "testuser", "Nuova versione");

        // Then
        assertThat(result).isNotNull();
        verify(noteVersionRepository).findMaxVersionNumber(1L);
        verify(noteVersionRepository).save(argThat(version ->
                version.getVersionNumber() == 2 &&
                        version.getChangeDescription().equals("Nuova versione") &&
                        version.getCreatedBy().equals("testuser")
        ));
    }

    @Test
    @DisplayName("VER-002: Dovrebbe creare la prima versione con numero 1")
    void shouldCreateFirstVersionWithNumber1() {
        // Given
        when(noteVersionRepository.findMaxVersionNumber(1L)).thenReturn(Optional.empty());
        when(noteVersionRepository.save(any(NoteVersion.class))).thenReturn(testVersion);

        // When
        NoteVersion result = noteVersionService.createVersion(testNote, "testuser", "Prima versione");

        // Then
        verify(noteVersionRepository).save(argThat(version -> version.getVersionNumber() == 1));
    }

    @Test
    @DisplayName("VER-003: Dovrebbe preservare tutti i metadati nella versione")
    void shouldPreserveAllMetadataInVersion() {
        // Given
        testNote.setTitolo("Titolo Complesso");
        testNote.setContenuto("Contenuto con caratteri speciali: àèìòù ©®™");
        when(noteVersionRepository.findMaxVersionNumber(1L)).thenReturn(Optional.of(0));
        when(noteVersionRepository.save(any(NoteVersion.class))).thenReturn(testVersion);

        // When
        noteVersionService.createVersion(testNote, "testuser", "Test metadati");

        // Then
        verify(noteVersionRepository).save(argThat(version ->
                version.getTitolo().equals("Titolo Complesso") &&
                        version.getContenuto().equals("Contenuto con caratteri speciali: àèìòù ©®™") &&
                        version.getCreatedBy().equals("testuser") &&
                        version.getChangeDescription().equals("Test metadati") &&
                        version.getCreatedAt() != null
        ));
    }

    // ===== TEST GESTIONE CRONOLOGIA =====

    @Test
    @DisplayName("VER-004: Dovrebbe ottenere la cronologia delle versioni ordinata")
    void shouldGetVersionHistoryOrdered() {
        // Given
        NoteVersion version1 = new NoteVersion(testNote, 1, "Contenuto v1", "Titolo v1", "testuser", "Prima");
        NoteVersion version2 = new NoteVersion(testNote, 2, "Contenuto v2", "Titolo v2", "testuser", "Seconda");
        NoteVersion version3 = new NoteVersion(testNote, 3, "Contenuto v3", "Titolo v3", "anotheruser", "Terza");

        List<NoteVersion> versions = Arrays.asList(version3, version2, version1); // Ordine decrescente
        when(noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(1L)).thenReturn(versions);

        // When
        List<NoteVersion> result = noteVersionService.getVersionHistory(1L);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getVersionNumber()).isEqualTo(3); // Più recente
        assertThat(result.get(1).getVersionNumber()).isEqualTo(2);
        assertThat(result.get(2).getVersionNumber()).isEqualTo(1); // Più vecchia
    }

    @Test
    @DisplayName("VER-005: Dovrebbe ottenere una versione specifica")
    void shouldGetSpecificVersion() {
        // Given
        when(noteVersionRepository.findByNoteIdAndVersionNumber(1L, 2)).thenReturn(Optional.of(testVersion));

        // When
        Optional<NoteVersion> result = noteVersionService.getVersion(1L, 2);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testVersion);
    }

    @Test
    @DisplayName("VER-006: Dovrebbe gestire richiesta di versione inesistente")
    void shouldHandleNonExistentVersion() {
        // Given
        when(noteVersionRepository.findByNoteIdAndVersionNumber(1L, 999)).thenReturn(Optional.empty());

        // When
        Optional<NoteVersion> result = noteVersionService.getVersion(1L, 999);

        // Then
        assertThat(result).isEmpty();
    }

    // ===== TEST LIMITAZIONI VERSIONI =====

    @Test
    @DisplayName("VER-007: Dovrebbe limitare il numero massimo di versioni")
    void shouldLimitMaxVersions() {
        // Given
        int maxVersions = 10;
        when(noteVersionRepository.countByNoteId(1L)).thenReturn(11L); // Oltre il limite

        List<NoteVersion> oldVersions = Arrays.asList(
                new NoteVersion(testNote, 1, "Old", "Old", "user", "Old version")
        );
        when(noteVersionRepository.findOldestVersions(1L, 1)).thenReturn(oldVersions);
        when(noteVersionRepository.findMaxVersionNumber(1L)).thenReturn(Optional.of(11));
        when(noteVersionRepository.save(any(NoteVersion.class))).thenReturn(testVersion);

        // When
        noteVersionService.createVersionWithLimit(testNote, "testuser", "New version", maxVersions);

        // Then
        verify(noteVersionRepository).deleteAll(oldVersions); // Rimuove versioni vecchie
        verify(noteVersionRepository).save(any(NoteVersion.class)); // Salva nuova versione
    }

    @Test
    @DisplayName("VER-008: Non dovrebbe rimuovere versioni se sotto il limite")
    void shouldNotRemoveVersionsIfUnderLimit() {
        // Given
        int maxVersions = 10;
        when(noteVersionRepository.countByNoteId(1L)).thenReturn(5L); // Sotto il limite
        when(noteVersionRepository.findMaxVersionNumber(1L)).thenReturn(Optional.of(5));
        when(noteVersionRepository.save(any(NoteVersion.class))).thenReturn(testVersion);

        // When
        noteVersionService.createVersionWithLimit(testNote, "testuser", "New version", maxVersions);

        // Then
        verify(noteVersionRepository, never()).findOldestVersions(anyLong(), anyInt());
        verify(noteVersionRepository, never()).deleteAll(anyList());
        verify(noteVersionRepository).save(any(NoteVersion.class));
    }

    // ===== TEST CONFRONTO VERSIONI =====

    @Test
    @DisplayName("VER-009: Dovrebbe calcolare differenze tra versioni")
    void shouldCalculateVersionDifferences() {
        // Given
        NoteVersion version1 = new NoteVersion(testNote, 1, "Contenuto originale", "Titolo originale", "user1", "Prima");
        NoteVersion version2 = new NoteVersion(testNote, 2, "Contenuto modificato", "Titolo modificato", "user2", "Seconda");

        // When
        VersionDifference diff = noteVersionService.calculateDifferences(version1, version2);

        // Then
        assertThat(diff.isTitleChanged()).isTrue();
        assertThat(diff.isContentChanged()).isTrue();
        assertThat(diff.getContentChanges()).contains("Contenuto");
        assertThat(diff.getTitleChanges()).contains("Titolo");
    }

    @Test
    @DisplayName("VER-010: Dovrebbe rilevare che non ci sono differenze")
    void shouldDetectNoDifferences() {
        // Given
        NoteVersion version1 = new NoteVersion(testNote, 1, "Stesso contenuto", "Stesso titolo", "user1", "Prima");
        NoteVersion version2 = new NoteVersion(testNote, 2, "Stesso contenuto", "Stesso titolo", "user2", "Seconda");

        // When
        VersionDifference diff = noteVersionService.calculateDifferences(version1, version2);

        // Then
        assertThat(diff.isTitleChanged()).isFalse();
        assertThat(diff.isContentChanged()).isFalse();
    }

    // ===== TEST PERFORMANCE =====

    @Test
    @DisplayName("VER-011: Dovrebbe essere performante con molte versioni")
    void shouldBePerformantWithManyVersions() {
        // Given
        List<NoteVersion> manyVersions = generateManyVersions(100);
        when(noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(1L)).thenReturn(manyVersions);

        // When
        long startTime = System.currentTimeMillis();
        List<NoteVersion> result = noteVersionService.getVersionHistory(1L);
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(result).hasSize(100);
        assertThat(endTime - startTime).isLessThan(50); // Meno di 50ms
    }

    @Test
    @DisplayName("VER-012: Dovrebbe gestire eliminazione massiva versioni")
    void shouldHandleBulkVersionDeletion() {
        // Given
        List<NoteVersion> versionsToDelete = generateManyVersions(50);

        // When
        noteVersionService.deleteVersionsBulk(versionsToDelete);

        // Then
        verify(noteVersionRepository).deleteAll(versionsToDelete);
    }

    // ===== TEST SICUREZZA =====

    @Test
    @DisplayName("VER-013: Dovrebbe tracciare l'autore delle modifiche")
    void shouldTrackModificationAuthor() {
        // Given
        when(noteVersionRepository.findMaxVersionNumber(1L)).thenReturn(Optional.of(1));
        when(noteVersionRepository.save(any(NoteVersion.class))).thenReturn(testVersion);

        // When
        noteVersionService.createVersion(testNote, "specificuser", "Modifica specifica");

        // Then
        verify(noteVersionRepository).save(argThat(version ->
                version.getCreatedBy().equals("specificuser")
        ));
    }

    @Test
    @DisplayName("VER-014: Dovrebbe gestire descrizione modifica nulla")
    void shouldHandleNullChangeDescription() {
        // Given
        when(noteVersionRepository.findMaxVersionNumber(1L)).thenReturn(Optional.of(1));
        when(noteVersionRepository.save(any(NoteVersion.class))).thenReturn(testVersion);

        // When
        noteVersionService.createVersion(testNote, "testuser", null);

        // Then
        verify(noteVersionRepository).save(argThat(version ->
                version.getChangeDescription() != null &&
                        !version.getChangeDescription().isEmpty()
        ));
    }

    // ===== METODI HELPER =====

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
        return note;
    }

    private List<NoteVersion> generateManyVersions(int count) {
        return java.util.stream.IntStream.range(1, count + 1)
                .mapToObj(i -> new NoteVersion(testNote, i, "Contenuto " + i, "Titolo " + i, "user" + (i % 3), "Versione " + i))
                .collect(java.util.stream.Collectors.toList());
    }

    // Classe helper per le differenze
    public static class VersionDifference {
        private final boolean titleChanged;
        private final boolean contentChanged;
        private final String titleChanges;
        private final String contentChanges;

        public VersionDifference(boolean titleChanged, boolean contentChanged, String titleChanges, String contentChanges) {
            this.titleChanged = titleChanged;
            this.contentChanged = contentChanged;
            this.titleChanges = titleChanges;
            this.contentChanges = contentChanges;
        }

        public boolean isTitleChanged() { return titleChanged; }
        public boolean isContentChanged() { return contentChanged; }
        public String getTitleChanges() { return titleChanges; }
        public String getContentChanges() { return contentChanges; }
    }
}