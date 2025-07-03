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
        List<NoteVersion> versions = noteVersionRepository.findVersionHistory(noteId);
        for (NoteVersion version : versions) {
            noteVersionRepository.delete(version);
        }
        System.out.println("Eliminate " + versions.size() + " versioni per la nota " + noteId);
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