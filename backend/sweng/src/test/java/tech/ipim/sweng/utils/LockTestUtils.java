package tech.ipim.sweng.utils;

import java.time.LocalDateTime;
import java.util.HashSet;

import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.NoteLock;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.model.User;

/**
 * Classe di utility per creare entità di test legate al sistema di lock delle note.
 * NON è una classe test per questo non ha @TEST, ma è una classe di utilita
 * <p>
 * Include metodi per generare oggetti {@link User}, {@link Note} e {@link NoteLock}
 * con valori di default utili nei test.
 */
public class LockTestUtils {

    /**
     * Crea un oggetto {@link User} di test con nome, cognome e password predefiniti.
     *
     * @param username nome utente da assegnare
     * @param email email da assegnare
     * @return istanza di {@link User} pronta per i test
     */
    public static User createTestUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setNome("Test");
        user.setCognome("User");
        return user;
    }


    /**
     * Crea una nota di test con autore, titolo, contenuto e tipo permesso specificati.
     * Inizializza anche i set di permessi, tag e cartelle per evitare null nei test.
     *
     * @param titolo titolo della nota
     * @param contenuto contenuto della nota
     * @param autore autore della nota (utente)
     * @param tipoPermesso tipo di permesso da applicare (pubblica, privata, ecc.)
     * @return oggetto {@link Note} pronto per i test
     */
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

    /**
     * Crea un {@link NoteLock} attivo con durata personalizzata a partire dall’orario corrente.
     *
     * @param noteId ID della nota da bloccare
     * @param username utente che ha attivato il lock
     * @param durationMinutes durata in minuti del blocco
     * @return istanza di {@link NoteLock} con scadenza futura
     */
    public static NoteLock createTestLock(Long noteId, String username, int durationMinutes) {
        LocalDateTime now = LocalDateTime.now();
        return new NoteLock(noteId, username, now, now.plusMinutes(durationMinutes));
    }


    /**
     * Crea un {@link NoteLock} già scaduto, utile per testare i comportamenti su lock non validi.
     *
     * @param noteId ID della nota da bloccare
     * @param username utente che aveva attivato il lock
     * @return istanza di {@link NoteLock} scaduta
     */
    public static NoteLock createExpiredLock(Long noteId, String username) {
        LocalDateTime past = LocalDateTime.now().minusMinutes(5);
        return new NoteLock(noteId, username, past, past.plusMinutes(1));
    }

    /**
     * Verifica se un lock è scaduto.
     *
     * @param lock il lock da controllare
     * @return true se il lock è scaduto, false altrimenti
     */
    public static boolean isLockExpired(NoteLock lock) {
        return lock.isExpired();
    }


    /**
     * Calcola i minuti rimanenti alla scadenza del lock.
     *
     * @param lock il lock su cui calcolare il tempo rimanente
     * @return minuti rimanenti alla scadenza, oppure 0 se la data è nulla
     */
    public static long getRemainingMinutes(NoteLock lock) {
        if (lock.getExpiresAt() == null) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), lock.getExpiresAt()).toMinutes();
    }
}