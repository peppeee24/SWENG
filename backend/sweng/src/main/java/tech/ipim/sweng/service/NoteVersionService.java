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

    public List<NoteVersion> getVersionHistory(Long noteId) {
        return noteVersionRepository.findVersionHistory(noteId);
    }

    public Optional<NoteVersion> getVersion(Long noteId, Integer versionNumber) {
        return noteVersionRepository.findByNoteIdAndVersionNumber(noteId, versionNumber);
    }


    @Transactional
    public void deleteAllVersionsForNote(Long noteId) {
        System.out.println("️ Eliminazione di tutte le versioni per nota ID: " + noteId);

        try {
            List<NoteVersion> versions = noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteId);
            System.out.println(" Trovate " + versions.size() + " versioni da eliminare");

            if (!versions.isEmpty()) {
                // Elimina tutte le versioni
                noteVersionRepository.deleteAll(versions);
                noteVersionRepository.flush(); // Forza l'eliminazione immediata
                System.out.println(" Eliminate " + versions.size() + " versioni per la nota " + noteId);
            } else {
                System.out.println("ℹ Nessuna versione trovata per la nota " + noteId);
            }

        } catch (Exception e) {
            System.err.println(" Errore eliminazione versioni per nota " + noteId + ": " + e.getMessage());
            throw new RuntimeException("Errore durante l'eliminazione delle versioni: " + e.getMessage());
        }
    }


    public Optional<NoteVersion> getLatestVersion(Long noteId) {
        List<NoteVersion> versions = noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteId);
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
    }

    private Integer getNextVersionNumber(Long noteId) {
        Optional<Integer> latestVersion = noteVersionRepository.findLatestVersionNumber(noteId);
        return latestVersion.orElse(0) + 1;
    }

    public boolean hasConflictingChanges(Long noteId, Integer baseVersion, String currentContent, String currentTitle) {
        Optional<NoteVersion> latestVersion = getLatestVersion(noteId);

        if (latestVersion.isEmpty()) {
            return false;
        }

        if (latestVersion.get().getVersionNumber() > baseVersion) {
            return !latestVersion.get().getContenuto().equals(currentContent) ||
                    !latestVersion.get().getTitolo().equals(currentTitle);
        }

        return false;
    }
}