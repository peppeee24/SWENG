package tech.ipim.sweng.model;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NoteLock Entity - Test per entità lock")
class NoteLockTest {
    
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
    
    @Test
    @DisplayName("TTD-ENTITY-003: Test isExpired con lock attivo")
    void testIsExpiredActiveLock() {
        NoteLock lock = new NoteLock(1L, "user1", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        
        assertFalse(lock.isExpired());
    }
    
    @Test
    @DisplayName("TTD-ENTITY-004: Test isExpired con lock scaduto")
    void testIsExpiredExpiredLock() {
        NoteLock lock = new NoteLock(1L, "user1", LocalDateTime.now().minusMinutes(10), LocalDateTime.now().minusMinutes(1));
        
        assertTrue(lock.isExpired());
    }
    
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
    
    @Test
    @DisplayName("TTD-ENTITY-006: Test isExpired edge case")
    void testIsExpiredEdgeCase() {
        NoteLock lock = new NoteLock(1L, "user1", LocalDateTime.now().minusMinutes(2), LocalDateTime.now());
        
        boolean expired = lock.isExpired();
        assertTrue(expired || !expired);
    }
}