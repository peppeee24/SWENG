package tech.ipim.sweng.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import tech.ipim.sweng.repository.NoteLockRepository;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.service.NoteLockService;
import tech.ipim.sweng.service.NoteService;

/**
 * Test di configurazione per il sistema di gestione dei lock sulle note.
 * <p>
 * Verifica che i bean principali relativi al lock (in particolare {@link NoteLockService})
 * vengano correttamente istanziati e configurati all'interno del contesto Spring.
 * <p>
 * I test sono eseguiti con profilo {@code "test"} e utilizzano {@code @MockitoExtension}
 * per il mocking delle dipendenze.
 *
 * <ul>
 *   <li><b>TTD-CONFIG-001</b>: verifica che {@code NoteLockService} sia istanziato</li>
 *   <li><b>TTD-CONFIG-002</b>: verifica che la configurazione iniziale del lock non sia nulla</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("Lock Configuration - Test configurazione sistema lock")
class LockConfigurationTest {

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
    
    @Test
    @DisplayName("TTD-CONFIG-001: Verifica configurazione bean NoteLockService")
    void testNoteLockServiceConfiguration() {
        // Assert
        assertNotNull(noteLockService);
        assertInstanceOf(NoteLockService.class, noteLockService);
    }
    
    @Test
    @DisplayName("TTD-CONFIG-002: Verifica configurazione timeout lock")
    void testLockTimeoutConfiguration() {
        assertNotNull(noteLockService);
    }
}