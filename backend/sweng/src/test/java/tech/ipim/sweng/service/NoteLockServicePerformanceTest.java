package tech.ipim.sweng.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("NoteLockService Performance - Test performance sistema lock")
class NoteLockServicePerformanceTest {

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
    @DisplayName("TTD-PERF-001: Test che il service esiste ed è veloce")
    void testServiceExists() {
    
        long startTime = System.currentTimeMillis();
        assertNotNull(noteLockService);
        long endTime = System.currentTimeMillis();
        
        
        assertTrue(endTime - startTime < 100); 
    }
    
    @Test
    @DisplayName("TTD-PERF-002: Test parametri null sono veloci")
    void testNullParametersPerformance() {
        // Act
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            try {
                noteLockService.tryLockNote(null, "user");
            } catch (Exception e) {
                
            }
        }
        
        long endTime = System.currentTimeMillis();
        
        
        assertTrue(endTime - startTime < 1000);
    }
}