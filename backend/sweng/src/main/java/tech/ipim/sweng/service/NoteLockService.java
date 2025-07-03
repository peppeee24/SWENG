package tech.ipim.sweng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tech.ipim.sweng.dto.LockStatusDto;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.repository.NoteRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class NoteLockService {

    @Value("${app.note.lock.duration-minutes:2}")
    private int lockDurationMinutes;

    @Autowired
    private NoteRepository noteRepository;

    public boolean tryLockNote(Long noteId, String username) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            throw new RuntimeException("Nota non trovata");
        }

        Note note = noteOpt.get();

        if (!note.hasWriteAccess(username)) {
            throw new RuntimeException("Non hai i permessi per modificare questa nota");
        }

        cleanExpiredLock(note);

        if (!note.canBeEditedBy(username)) {
            return false;
        }

        note.lockFor(username, lockDurationMinutes);
        noteRepository.save(note);

        System.out.println("Nota " + noteId + " bloccata per utente " + username +
                " fino a " + note.getLockExpiresAt());

        return true;
    }

    public void unlockNote(Long noteId, String username) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return;
        }

        Note note = noteOpt.get();

        if (note.isLockedBy(username)) {
            note.unlock();
            noteRepository.save(note);
            System.out.println("Nota " + noteId + " sbloccata da utente " + username);
        }
    }

    public boolean refreshLock(Long noteId, String username) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return false;
        }

        Note note = noteOpt.get();

        if (note.isLockedBy(username)) {
            note.lockFor(username, lockDurationMinutes);
            noteRepository.save(note);
            System.out.println("Lock rinnovato per nota " + noteId + " utente " + username);
            return true;
        }

        return false;
    }

    public boolean isNoteLocked(Long noteId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return false;
        }

        Note note = noteOpt.get();
        cleanExpiredLock(note);
        return note.isLocked();
    }

    public String getNoteLockOwner(Long noteId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return null;
        }

        Note note = noteOpt.get();
        cleanExpiredLock(note);
        return note.isLocked() ? note.getLockedByUser() : null;
    }

    public boolean canUserEditNote(Long noteId, String username) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return false;
        }

        Note note = noteOpt.get();
        cleanExpiredLock(note);
        return note.canBeEditedBy(username);
    }

    public void extendNoteLock(Long noteId, String username) {
        refreshLock(noteId, username);
    }

    public LockStatusDto getLockStatus(Long noteId, String username) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            throw new RuntimeException("Nota non trovata");
        }

        Note note = noteOpt.get();
        cleanExpiredLock(note);

        if (!note.isLocked()) {
            boolean canEdit = note.hasWriteAccess(username);
            return new LockStatusDto(false, null, null, canEdit);
        }

        boolean canEdit = note.isLockedBy(username);
        return new LockStatusDto(
                true,
                note.getLockedByUser(),
                note.getLockExpiresAt(),
                canEdit
        );
    }

    private void cleanExpiredLock(Note note) {
        if (note.hasExpiredLock()) {
            note.unlock();
            noteRepository.save(note);
            System.out.println("Lock scaduto rimosso per nota " + note.getId());
        }
    }

    public void forceUnlockNote(Long noteId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isPresent()) {
            Note note = noteOpt.get();
            note.unlock();
            noteRepository.save(note);
            System.out.println("Lock forzatamente rimosso per nota " + noteId);
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanExpiredLocks() {
        noteRepository.findAll().forEach(note -> {
            if (note.hasExpiredLock()) {
                note.unlock();
                noteRepository.save(note);
                System.out.println("Lock scaduto rimosso automaticamente per nota " + note.getId());
            }
        });
    }
}