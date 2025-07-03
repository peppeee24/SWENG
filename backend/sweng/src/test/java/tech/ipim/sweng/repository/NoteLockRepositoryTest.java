package tech.ipim.sweng.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import tech.ipim.sweng.model.NoteLock;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("NoteLockRepository - Test per operazioni database lock")
class NoteLockRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private NoteLockRepository noteLockRepository;
    
    private NoteLock testLock;
    
    @BeforeEach
    void setUp() {
        testLock = new NoteLock(1L, "testuser", LocalDateTime.now(), LocalDateTime.now().plusMinutes(2));
    }
    
    @Test
    @DisplayName("TTD-REPO-001: Test salvataggio lock")
    void testSaveLock() {
        NoteLock savedLock = noteLockRepository.save(testLock);
        
        assertNotNull(savedLock.getId());
        assertEquals(1L, savedLock.getNoteId());
        assertEquals("testuser", savedLock.getLockedBy());
        assertNotNull(savedLock.getLockedAt());
        assertNotNull(savedLock.getExpiresAt());
    }
    
    @Test
    @DisplayName("TTD-REPO-002: Test ricerca per ID")
    void testFindById() {
        NoteLock savedLock = entityManager.persistAndFlush(testLock);
        
        Optional<NoteLock> found = noteLockRepository.findById(savedLock.getId());
        
        assertTrue(found.isPresent());
        assertEquals(savedLock.getId(), found.get().getId());
        assertEquals(1L, found.get().getNoteId());
        assertEquals("testuser", found.get().getLockedBy());
    }
    
    @Test
    @DisplayName("TTD-REPO-003: Test eliminazione lock")
    void testDeleteLock() {
        NoteLock savedLock = entityManager.persistAndFlush(testLock);
        
        noteLockRepository.delete(savedLock);
        entityManager.flush();
        
        Optional<NoteLock> found = noteLockRepository.findById(savedLock.getId());
        assertFalse(found.isPresent());
    }
    
    @Test
    @DisplayName("TTD-REPO-004: Test conta tutti i lock")
    void testCountAll() {
        entityManager.persistAndFlush(testLock);
        
        long count = noteLockRepository.count();
        
        assertEquals(1, count);
    }
    
    @Test
    @DisplayName("TTD-REPO-005: Test constraint unique su noteId")
    void testUniqueConstraintOnNoteId() {
        entityManager.persistAndFlush(testLock);
        
        NoteLock duplicateLock = new NoteLock(1L, "otheruser", LocalDateTime.now(), LocalDateTime.now().plusMinutes(1));
        
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(duplicateLock);
        });
    }
    
    @Test
    @DisplayName("TTD-REPO-006: Test findAll restituisce lista")
    void testFindAll() {
        entityManager.persistAndFlush(testLock);
        
        var allLocks = noteLockRepository.findAll();
        
        assertNotNull(allLocks);
        assertEquals(1, allLocks.size());
        assertEquals(testLock.getNoteId(), allLocks.get(0).getNoteId());
    }
}