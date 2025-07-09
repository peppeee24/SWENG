package tech.ipim.sweng.dto;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


/**
 * Test unitari per {@link LockStatusDto}, verificando il corretto funzionamento
 * del DTO utilizzato per rappresentare lo stato di blocco delle note.
 * <p>
 * Verifica il comportamento dei costruttori, getters, setters e dei diversi
 * stati possibili del lock (bloccato/sbloccato, proprietario del lock, permessi di modifica).
 * <p>
 * Questi test assicurano che il DTO mantenga correttamente le informazioni
 * necessarie per la gestione del sistema di lock delle note durante la modifica collaborativa.
 * <p>
 * <p>
 * Riepilogo dei test implementati:
 * <ul>
 *   <li>{@code testConstructorWithParameters} – Test costruttore con tutti i parametri</li>
 *   <li>{@code testDefaultConstructor} – Test costruttore vuoto e valori di default</li>
 *   <li>{@code testSettersGetters} – Test funzionamento di setters e getters</li>
 *   <li>{@code testUnlockedState} – Test rappresentazione stato nota non bloccata</li>
 *   <li>{@code testLockedByOtherUser} – Test rappresentazione stato nota bloccata da altro utente</li>
 * </ul>
 */
@DisplayName("LockStatusDto - Test per DTO stato lock")
class LockStatusDtoTest {

    /**
     * Verifica che il costruttore con parametri inizializzi correttamente
     * tutti i campi del DTO con i valori forniti.
     *
     * Testa il caso di una nota bloccata da un utente specifico con
     * scadenza del lock e permessi di modifica attivi.
     */
    @Test
    @DisplayName("TTD-DTO-001: Test costruttore con parametri")
    void testConstructorWithParameters() {
        boolean isLocked = true;
        String lockedBy = "testuser";
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        boolean canEdit = true;
        
        LockStatusDto dto = new LockStatusDto(isLocked, lockedBy, expiresAt, canEdit);
        
        assertTrue(dto.isLocked());
        assertEquals("testuser", dto.getLockedBy());
        assertEquals(expiresAt, dto.getLockExpiresAt());
        assertTrue(dto.canEdit());
    }

    /**
     * Verifica che il costruttore vuoto crei un'istanza valida del DTO
     * con valori di default appropriati per uno stato non bloccato.
     *
     * Controlla che tutti i campi abbiano valori coerenti con una nota
     * non in stato di lock (false/null per i campi appropriati).
     */
    @Test
    @DisplayName("TTD-DTO-002: Test costruttore vuoto")
    void testDefaultConstructor() {
        LockStatusDto dto = new LockStatusDto();
        
        assertNotNull(dto);
        assertFalse(dto.isLocked());
        assertNull(dto.getLockedBy());
        assertNull(dto.getLockExpiresAt());
        assertFalse(dto.canEdit());
    }

    /**
     * Verifica il corretto funzionamento di tutti i metodi setter e getter
     * del DTO, assicurando che i valori impostati vengano recuperati correttamente.
     *
     * Testa la modifica di tutti i campi dopo la creazione dell'oggetto
     * e la coerenza dei dati memorizzati.
     */
    @Test
    @DisplayName("TTD-DTO-003: Test setters e getters")
    void testSettersGetters() {
        LockStatusDto dto = new LockStatusDto();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(3);
        
        dto.setLocked(true);
        dto.setLockedBy("user123");
        dto.setLockExpiresAt(expiresAt);
        dto.setCanEdit(false);
        
        assertTrue(dto.isLocked());
        assertEquals("user123", dto.getLockedBy());
        assertEquals(expiresAt, dto.getLockExpiresAt());
        assertFalse(dto.canEdit());
    }


    /**
     * Verifica la corretta rappresentazione dello stato di una nota
     * non bloccata e disponibile per la modifica.
     *
     * Controlla che i valori riflettano accuratamente una situazione
     * in cui la nota può essere modificata liberamente dall'utente.
     */
    @Test
    @DisplayName("TTD-DTO-004: Test stato nota non bloccata")
    void testUnlockedState() {
        LockStatusDto dto = new LockStatusDto(false, null, null, true);
        
        assertFalse(dto.isLocked());
        assertNull(dto.getLockedBy());
        assertNull(dto.getLockExpiresAt());
        assertTrue(dto.canEdit());
    }

    /**
     * Verifica la corretta rappresentazione dello stato di una nota
     * bloccata da un altro utente e non modificabile.
     *
     * Testa il caso in cui un utente diverso ha acquisito il lock
     * sulla nota, impedendo modifiche e mostrando informazioni
     * sul proprietario del lock e la scadenza.
     */
    @Test
    @DisplayName("TTD-DTO-005: Test stato nota bloccata da altro utente")
    void testLockedByOtherUser() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(2);
        
        LockStatusDto dto = new LockStatusDto(true, "otheruser", expiresAt, false);
        
        assertTrue(dto.isLocked());
        assertEquals("otheruser", dto.getLockedBy());
        assertEquals(expiresAt, dto.getLockExpiresAt());
        assertFalse(dto.canEdit());
    }
}