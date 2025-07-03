package tech.ipim.sweng.utils;

import java.time.LocalDateTime;
import java.util.HashSet;

import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.NoteLock;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.model.User;


public class LockTestUtils {
    
    public static User createTestUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setNome("Test");
        user.setCognome("User");
        return user;
    }
    
    public static Note createTestNote(String titolo, String contenuto, User autore, TipoPermesso tipoPermesso) {
        Note note = new Note();
        note.setTitolo(titolo);
        note.setContenuto(contenuto);
        note.setAutore(autore);
        note.setTipoPermesso(tipoPermesso);
        note.setDataCreazione(LocalDateTime.now());
        note.setDataModifica(LocalDateTime.now());
        // Inizializza i Set per evitare null
        note.setPermessiLettura(new HashSet<>());
        note.setPermessiScrittura(new HashSet<>());
        note.setTags(new HashSet<>());
        note.setCartelle(new HashSet<>());
        return note;
    }
    
    public static NoteLock createTestLock(Long noteId, String username, int durationMinutes) {
        LocalDateTime now = LocalDateTime.now();
        return new NoteLock(noteId, username, now, now.plusMinutes(durationMinutes));
    }
    
    public static NoteLock createExpiredLock(Long noteId, String username) {
        LocalDateTime past = LocalDateTime.now().minusMinutes(5);
        return new NoteLock(noteId, username, past, past.plusMinutes(1));
    }
    
    public static boolean isLockExpired(NoteLock lock) {
        return lock.isExpired();
    }
    
    
    public static long getRemainingMinutes(NoteLock lock) {
        if (lock.getExpiresAt() == null) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), lock.getExpiresAt()).toMinutes();
    }
}