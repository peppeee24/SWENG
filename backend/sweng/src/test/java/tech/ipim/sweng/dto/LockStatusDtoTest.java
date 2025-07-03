package tech.ipim.sweng.dto;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LockStatusDto - Test per DTO stato lock")
class LockStatusDtoTest {

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
    
    @Test
    @DisplayName("TTD-DTO-004: Test stato nota non bloccata")
    void testUnlockedState() {
        LockStatusDto dto = new LockStatusDto(false, null, null, true);
        
        assertFalse(dto.isLocked());
        assertNull(dto.getLockedBy());
        assertNull(dto.getLockExpiresAt());
        assertTrue(dto.canEdit());
    }
    
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