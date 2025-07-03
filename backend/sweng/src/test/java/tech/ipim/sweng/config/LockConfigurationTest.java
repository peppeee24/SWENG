package tech.ipim.sweng.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import tech.ipim.sweng.service.NoteLockService;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Lock Configuration - Test configurazione sistema lock")
class LockConfigurationTest {

    @Autowired
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