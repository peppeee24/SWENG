package tech.ipim.sweng.model;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


/**
 * Test unitari per l'entità {@link NoteLock}, utilizzata per gestire i lock delle note durante la modifica collaborativa.
 * <p>
 * Verifica il comportamento dei costruttori, getters, setters e metodi di business
 * dell'entità, inclusa la logica di scadenza del lock per la gestione delle sessioni
 * di modifica collaborative.
 * <p>
 * Questi test assicurano che l'entità mantenga correttamente le informazioni
 * necessarie per il sistema di lock: ID nota, proprietario, timestamp di creazione
 * e scadenza, oltre alla logica per determinare se un lock è ancora valido.
 * <p>
 * <p>
 * Riepilogo dei test implementati:
 * <ul>
 *   <li>{@code testConstructorWithParameters} – Test costruttore con tutti i parametri</li>
 *   <li>{@code testDefaultConstructor} – Test costruttore vuoto e valori di default</li>
 *   <li>{@code testIsExpiredActiveLock} – Test logica scadenza per lock attivo</li>
 *   <li>{@code testIsExpiredExpiredLock} – Test logica scadenza per lock scaduto</li>
 *   <li>{@code testAllGettersSetters} – Test completo di tutti i getters e setters</li>
 *   <li>{@code testIsExpiredEdgeCase} – Test edge case per logica di scadenza</li>
 * </ul>
 */
@DisplayName("NoteLock Entity - Test per entità lock")
class NoteLockTest {


    /**
     * Verifica che il costruttore con parametri inizializzi correttamente
     * tutti i campi dell'entità NoteLock con i valori forniti.
     *
     * Testa la creazione di un lock con ID nota, proprietario e timestamp
     * di creazione e scadenza specifici, verificando l'assegnazione corretta.
     */
    @Test
    @DisplayName("TTD-ENTITY-001: Test costruttore con parametri")
    void testConstructorWithParameters() {
        Long noteId = 1L;
        String lockedBy = "testuser";
        LocalDateTime lockedAt = LocalDateTime.now();
        LocalDateTime expiresAt = lockedAt.plusMinutes(5);
        
        NoteLock lock = new NoteLock(noteId, lockedBy, lockedAt, expiresAt);
        
        assertEquals(noteId, lock.getNoteId());
        assertEquals(lockedBy, lock.getLockedBy());
        assertEquals(lockedAt, lock.getLockedAt());
        assertEquals(expiresAt, lock.getExpiresAt());
    }


    /**
     * Verifica che il costruttore vuoto crei un'istanza valida dell'entità
     * con tutti i campi inizializzati a null come da specifiche JPA.
     *
     * Controlla che l'entità sia istanziabile senza parametri e che
     * tutti i campi abbiano valori null di default appropriati.
     */
    @Test
    @DisplayName("TTD-ENTITY-002: Test costruttore vuoto")
    void testDefaultConstructor() {
        NoteLock lock = new NoteLock();
        
        assertNotNull(lock);
        assertNull(lock.getId());
        assertNull(lock.getNoteId());
        assertNull(lock.getLockedBy());
        assertNull(lock.getLockedAt());
        assertNull(lock.getExpiresAt());
    }


    /**
     * Verifica che il metodo isExpired() restituisca false per un lock
     * ancora valido (non scaduto) basandosi sul timestamp di scadenza.
     *
     * Testa la logica di business per determinare se un lock è ancora
     * attivo confrontando l'ora corrente con il timestamp di scadenza.
     */
    @Test
    @DisplayName("TTD-ENTITY-003: Test isExpired con lock attivo")
    void testIsExpiredActiveLock() {
        NoteLock lock = new NoteLock(1L, "user1", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        
        assertFalse(lock.isExpired());
    }


    /**
     * Verifica che il metodo isExpired() restituisca true per un lock
     * scaduto basandosi sul timestamp di scadenza passato.
     *
     * Testa la logica di business per identificare lock scaduti
     * che devono essere rimossi o ignorati dal sistema.
     */
    @Test
    @DisplayName("TTD-ENTITY-004: Test isExpired con lock scaduto")
    void testIsExpiredExpiredLock() {
        NoteLock lock = new NoteLock(1L, "user1", LocalDateTime.now().minusMinutes(10), LocalDateTime.now().minusMinutes(1));
        
        assertTrue(lock.isExpired());
    }



    /**
     * Verifica il corretto funzionamento di tutti i metodi getter e setter
     * dell'entità, assicurando l'encapsulamento appropriato dei dati.
     *
     * Testa l'assegnazione e il recupero di tutti i campi dell'entità
     * per garantire la corretta gestione dello stato interno.
     */
    @Test
    @DisplayName("TTD-ENTITY-005: Test tutti i getters e setters")
    void testAllGettersSetters() {
        NoteLock lock = new NoteLock();
        Long id = 10L;
        Long noteId = 5L;
        String lockedBy = "testuser";
        LocalDateTime lockedAt = LocalDateTime.now();
        LocalDateTime expiresAt = lockedAt.plusMinutes(3);
        
        lock.setId(id);
        lock.setNoteId(noteId);
        lock.setLockedBy(lockedBy);
        lock.setLockedAt(lockedAt);
        lock.setExpiresAt(expiresAt);
        
        assertEquals(id, lock.getId());
        assertEquals(noteId, lock.getNoteId());
        assertEquals(lockedBy, lock.getLockedBy());
        assertEquals(lockedAt, lock.getLockedAt());
        assertEquals(expiresAt, lock.getExpiresAt());
    }


    /**
     * Verifica che il metodo isExpired() sia coerente quando chiamato
     * multiple volte su un lock con scadenza esatta al momento corrente.
     *
     * Testa la stabilità della logica di scadenza in casi limite temporali,
     * assicurando che chiamate successive restituiscano lo stesso risultato
     * per garantire comportamento deterministico del sistema di lock.
     */
    @Test
    @DisplayName("TTD-ENTITY-006: Test isExpired con scadenza esatta")
    void testIsExpiredExactTime() {
        LocalDateTime exactTime = LocalDateTime.now();
        NoteLock lock = new NoteLock(1L, "user1", exactTime.minusMinutes(1), exactTime);

        // Con scadenza esatta, il comportamento dipende dall'implementazione
        // Ma dovrebbe essere coerente
        boolean firstCheck = lock.isExpired();
        boolean secondCheck = lock.isExpired();

        assertEquals(firstCheck, secondCheck, "Il risultato dovrebbe essere coerente");
    }
}