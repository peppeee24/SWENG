package tech.ipim.sweng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tech.ipim.sweng.dto.LockStatusDto;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.repository.NoteRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@Transactional
public class NoteLockService {

    @Value("${app.note.lock.duration-minutes:2}")
    private int lockDurationMinutes;

    @Autowired
    private NoteRepository noteRepository;

    /**
     * Tenta di bloccare una nota per l'editing da parte di un utente
     * 
     * @param noteId ID della nota da bloccare
     * @param username username dell'utente che vuole bloccare la nota
     * @return true se il blocco è stato ottenuto, false se la nota è già bloccata da un altro utente
     * @throws RuntimeException se la nota non esiste o l'utente non ha permessi di scrittura
     */
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

        System.out.println("Nota " + noteId + " bloccata per utente " + username
                + " fino a " + note.getLockExpiresAt());

        return true;
    }

    /**
     * Sblocca una nota bloccata dall'utente specificato
     * 
     * @param noteId ID della nota da sbloccare
     * @param username username dell'utente che vuole sbloccare la nota
     */
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

    /**
     * Rinnova il blocco di una nota per l'utente specificato
     * 
     * @param noteId ID della nota da rinnovare
     * @param username username dell'utente che ha il blocco attivo
     * @return true se il rinnovo è avvenuto con successo, false altrimenti
     */
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

    /**
     * Verifica se una nota è attualmente bloccata
     * 
     * @param noteId ID della nota
     * @return true se la nota è bloccata, false altrimenti
     */
    public boolean isNoteLocked(Long noteId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return false;
        }

        Note note = noteOpt.get();
        cleanExpiredLock(note);
        return note.isLocked();
    }

    /**
     * Restituisce l'username dell'utente che ha bloccato la nota
     * 
     * @param noteId ID della nota
     * @return username del bloccatore oppure null se la nota non è bloccata o non esiste
     */
    public String getNoteLockOwner(Long noteId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return null;
        }

        Note note = noteOpt.get();
        cleanExpiredLock(note);
        return note.isLocked() ? note.getLockedByUser() : null;
    }

    /**
     * Verifica se un utente può modificare una nota (controlla permessi e blocco)
     * 
     * @param noteId ID della nota
     * @param username username dell'utente
     * @return true se l'utente può modificare la nota, false altrimenti
     */
    public boolean canUserEditNote(Long noteId, String username) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            return false;
        }

        Note note = noteOpt.get();
        cleanExpiredLock(note);
        return note.canBeEditedBy(username);
    }

    /**
     * Estende il blocco della nota per l'utente che ha già il lock
     * 
     * @param noteId ID della nota
     * @param username username dell'utente che estende il lock
     */
    public void extendNoteLock(Long noteId, String username) {
        refreshLock(noteId, username);
    }

    /**
     * Ottiene lo stato di blocco della nota per un utente specifico
     * 
     * @param noteId ID della nota
     * @param username username dell'utente che richiede lo stato
     * @return DTO con informazioni sullo stato del lock (bloccata o no, proprietario, scadenza, permesso di modifica)
     * @throws RuntimeException se la nota non esiste
     */
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

    /**
     * Rimuove il blocco scaduto da una nota, se presente
     * 
     * @param note la nota da controllare e sbloccare se necessario
     */
    private void cleanExpiredLock(Note note) {
        if (note.hasExpiredLock()) {
            note.unlock();
            noteRepository.save(note);
            System.out.println("Lock scaduto rimosso per nota " + note.getId());
        }
    }

    /**
     * Forza la rimozione del blocco su una nota, indipendentemente dall'utente
     * 
     * @param noteId ID della nota da sbloccare
     */
    public void forceUnlockNote(Long noteId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isPresent()) {
            Note note = noteOpt.get();
            note.unlock();
            noteRepository.save(note);
            System.out.println("Lock forzatamente rimosso per nota " + noteId);
        }
    }

    /**
     * Pulizia automatica periodica dei lock scaduti su tutte le note
     * Viene eseguita ogni 5 minuti (300000 ms)
     */
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
