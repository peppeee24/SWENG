package tech.ipim.sweng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.repository.NoteRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NoteLockService {

    @Autowired
    private NoteRepository noteRepository;

    private static final int LOCK_DURATION_MINUTES = 10;

    @Transactional
    public LockResult acquireLock(Long noteId, String username) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return new LockResult(false, "Nota non trovata", null, null);
        }

        Note note = noteOpt.get();

        if (!note.hasWriteAccess(username)) {
            return new LockResult(false, "Permessi insufficienti per modificare questa nota", null, null);
        }

        if (note.getIsLockedForEditing() && !note.isExpiredLock()) {
            if (!note.getLockedByUser().equals(username)) {
                return new LockResult(false,
                        "Nota attualmente in modifica da " + note.getLockedByUser(),
                        note.getLockedByUser(),
                        note.getLockExpiresAt());
            }
            return new LockResult(true, "Lock già acquisito dall'utente", username, note.getLockExpiresAt());
        }

        LocalDateTime lockExpiration = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
        note.setIsLockedForEditing(true);
        note.setLockedByUser(username);
        note.setLockExpiresAt(lockExpiration);

        noteRepository.save(note);

        return new LockResult(true, "Lock acquisito con successo", username, lockExpiration);
    }

    @Transactional
    public LockResult releaseLock(Long noteId, String username) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return new LockResult(false, "Nota non trovata", null, null);
        }

        Note note = noteOpt.get();

        if (!note.getIsLockedForEditing()) {
            return new LockResult(true, "Nota non era bloccata", null, null);
        }

        if (!note.getLockedByUser().equals(username)) {
            return new LockResult(false, "Non puoi rilasciare un lock di un altro utente", note.getLockedByUser(), note.getLockExpiresAt());
        }

        note.setIsLockedForEditing(false);
        note.setLockedByUser(null);
        note.setLockExpiresAt(null);

        noteRepository.save(note);

        return new LockResult(true, "Lock rilasciato con successo", null, null);
    }

    @Transactional
    public void releaseExpiredLocks() {
        noteRepository.findExpiredLocks(LocalDateTime.now())
                .forEach(note -> {
                    note.setIsLockedForEditing(false);
                    note.setLockedByUser(null);
                    note.setLockExpiresAt(null);
                    noteRepository.save(note);
                });
    }

    @Transactional(readOnly = true)
    public LockStatus getLockStatus(Long noteId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return new LockStatus(false, null, null);
        }

        Note note = noteOpt.get();

        if (!note.getIsLockedForEditing() || note.isExpiredLock()) {
            return new LockStatus(false, null, null);
        }

        return new LockStatus(true, note.getLockedByUser(), note.getLockExpiresAt());
    }

    public static class LockResult {
        private final boolean success;
        private final String message;
        private final String lockedByUser;
        private final LocalDateTime lockExpiresAt;

        public LockResult(boolean success, String message, String lockedByUser, LocalDateTime lockExpiresAt) {
            this.success = success;
            this.message = message;
            this.lockedByUser = lockedByUser;
            this.lockExpiresAt = lockExpiresAt;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getLockedByUser() {
            return lockedByUser;
        }

        public LocalDateTime getLockExpiresAt() {
            return lockExpiresAt;
        }
    }

    public static class LockStatus {
        private final boolean locked;
        private final String lockedByUser;
        private final LocalDateTime lockExpiresAt;

        public LockStatus(boolean locked, String lockedByUser, LocalDateTime lockExpiresAt) {
            this.locked = locked;
            this.lockedByUser = lockedByUser;
            this.lockExpiresAt = lockExpiresAt;
        }

        public boolean isLocked() {
            return locked;
        }

        public String getLockedByUser() {
            return lockedByUser;
        }

        public LocalDateTime getLockExpiresAt() {
            return lockExpiresAt;
        }
    }
}