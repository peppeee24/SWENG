package tech.ipim.sweng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.NoteVersion;
import tech.ipim.sweng.repository.NoteVersionRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NoteVersionService {

    private final NoteVersionRepository noteVersionRepository;

    @Autowired
    public NoteVersionService(NoteVersionRepository noteVersionRepository) {
        this.noteVersionRepository = noteVersionRepository;
    }

    /**
     * Crea una nuova versione della nota specificata.
     *
     * @param note               La nota di cui creare la versione
     * @param username           L'utente che ha effettuato la modifica
     * @param changeDescription  Descrizione della modifica effettuata
     * @return La nuova versione salvata della nota
     */
    @Transactional
    public NoteVersion createVersion(Note note, String username, String changeDescription) {
        Integer nextVersionNumber = getNextVersionNumber(note.getId());

        NoteVersion version = new NoteVersion(
                note,
                nextVersionNumber,
                note.getContenuto(),
                note.getTitolo(),
                username,
                changeDescription
        );

        return noteVersionRepository.save(version);
    }

    /**
     * Recupera lo storico di tutte le versioni di una nota.
     *
     * @param noteId ID della nota di cui recuperare le versioni
     * @return Lista ordinata di versioni associate alla nota
     */
    public List<NoteVersion> getVersionHistory(Long noteId) {
        return noteVersionRepository.findVersionHistory(noteId);
    }

    /**
     * Recupera una specifica versione di una nota.
     *
     * @param noteId        ID della nota
     * @param versionNumber Numero della versione da recuperare
     * @return Optional contenente la versione se trovata
     */
    public Optional<NoteVersion> getVersion(Long noteId, Integer versionNumber) {
        return noteVersionRepository.findByNoteIdAndVersionNumber(noteId, versionNumber);
    }

    /**
     * Elimina tutte le versioni associate a una nota.
     *
     * @param noteId ID della nota di cui eliminare le versioni
     */
    @Transactional
    public void deleteAllVersionsForNote(Long noteId) {
        System.out.println("️ Eliminazione di tutte le versioni per nota ID: " + noteId);

        try {
            List<NoteVersion> versions = noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteId);
            System.out.println(" Trovate " + versions.size() + " versioni da eliminare");

            if (!versions.isEmpty()) {
                noteVersionRepository.deleteAll(versions);
                noteVersionRepository.flush();
                System.out.println(" Eliminate " + versions.size() + " versioni per la nota " + noteId);
            } else {
                System.out.println("ℹ Nessuna versione trovata per la nota " + noteId);
            }

        } catch (Exception e) {
            System.err.println(" Errore eliminazione versioni per nota " + noteId + ": " + e.getMessage());
            throw new RuntimeException("Errore durante l'eliminazione delle versioni: " + e.getMessage());
        }
    }

    /**
     * Recupera l'ultima versione disponibile per una nota.
     *
     * @param noteId ID della nota
     * @return Optional contenente l'ultima versione se esistente
     */
    public Optional<NoteVersion> getLatestVersion(Long noteId) {
        List<NoteVersion> versions = noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteId);
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
    }

    /**
     * Calcola il numero della prossima versione disponibile per una nota.
     *
     * @param noteId ID della nota
     * @return Numero progressivo della prossima versione
     */
    private Integer getNextVersionNumber(Long noteId) {
        Optional<Integer> latestVersion = noteVersionRepository.findLatestVersionNumber(noteId);
        return latestVersion.orElse(0) + 1;
    }

    /**
     * Verifica se esistono conflitti tra la versione corrente della nota e la base di confronto.
     *
     * @param noteId        ID della nota
     * @param baseVersion   Numero della versione base di confronto
     * @param currentContent Contenuto corrente della nota
     * @param currentTitle   Titolo corrente della nota
     * @return True se esistono conflitti rispetto alla versione più recente, false altrimenti
     */
    public boolean hasConflictingChanges(Long noteId, Integer baseVersion, String currentContent, String currentTitle) {
        Optional<NoteVersion> latestVersion = getLatestVersion(noteId);

        if (latestVersion.isEmpty()) {
            return false;
        }

        if (latestVersion.get().getVersionNumber() > baseVersion) {
            return !latestVersion.get().getContenuto().equals(currentContent)
                    || !latestVersion.get().getTitolo().equals(currentTitle);
        }

        return false;
    }
}
