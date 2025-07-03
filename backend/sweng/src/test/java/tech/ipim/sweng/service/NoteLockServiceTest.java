package tech.ipim.sweng.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import tech.ipim.sweng.dto.LockStatusDto;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.NoteLock;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteLockRepository;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("NoteLockService - Test con metodi reali")
class NoteLockServiceTest {

    @Mock
    private NoteLockRepository noteLockRepository;
    
    @Mock
    private NoteRepository noteRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private NoteService noteService;
    
    @InjectMocks
    private NoteLockService noteLockService;
    
    private Note testNote;
    private User testUser;
    private NoteLock testLock;
    
    @BeforeEach
    void setUp() {
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user1");
        testUser.setEmail("user1@test.com");
        
        
        testNote = new Note();
        testNote.setId(1L);
        testNote.setTitolo("Test Note");
        testNote.setContenuto("Test Content");
        testNote.setAutore(testUser);
        testNote.setTipoPermesso(TipoPermesso.CONDIVISA_SCRITTURA);
        testNote.setPermessiLettura(new HashSet<>());
        testNote.setPermessiScrittura(new HashSet<>());
        testNote.setDataCreazione(LocalDateTime.now());
        testNote.setDataModifica(LocalDateTime.now());
        
        
        testLock = new NoteLock(1L, "user1", LocalDateTime.now(), LocalDateTime.now().plusMinutes(2));
        testLock.setId(1L);
    }
    
    @Test
    @DisplayName("TTD-LOCK-001: Test NoteLockService può essere istanziato")
    void testNoteLockServiceExists() {
        assertNotNull(noteLockService);
    }
    
    @Test
    @DisplayName("TTD-LOCK-002: Test tryLockNote con parametri null lancia RuntimeException")
    void testNullParameters() {
        
        assertThrows(RuntimeException.class, () -> {
            noteLockService.tryLockNote(null, "user1");
        });
        
        assertThrows(RuntimeException.class, () -> {
            noteLockService.tryLockNote(1L, null);
        });
    }
    
    @Test
    @DisplayName("TTD-LOCK-003: Test tryLockNote con nota inesistente")
    void testTryLockNote_NoteNotFound() {
        
        when(noteRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> {
            noteLockService.tryLockNote(999L, "user1");
        });
    }
    
    @Test
    @DisplayName("TTD-LOCK-004: Test tryLockNote con utente inesistente")
    void testTryLockNote_UserNotFound() {
        
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));
        
        assertThrows(RuntimeException.class, () -> {
            noteLockService.tryLockNote(1L, "nonexistent");
        });
    }
    
    @Test
    @DisplayName("TTD-LOCK-005: Test getLockStatus con parametri validi")
    void testGetLockStatus_ValidParameters() {
        
        when(noteRepository.findById(1L)).thenReturn(Optional.of(testNote));
        
        assertDoesNotThrow(() -> {
            LockStatusDto result = noteLockService.getLockStatus(1L, "user1");
            assertNotNull(result);
        });
    }
}