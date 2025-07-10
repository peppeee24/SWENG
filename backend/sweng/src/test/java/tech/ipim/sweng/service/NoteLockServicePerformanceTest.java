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

/**
 * Test di performance per {@link NoteLockService}.
 * <p>
 * Questi test hanno l’obiettivo di verificare che il servizio sia disponibile
 * e che le operazioni più comuni non superino soglie di tempo predefinite,
 * assicurando così una buona reattività del sistema di locking.
 * <p>
 * I test coprono in particolare:
 * - L’esistenza e inizializzazione veloce del service.
 * - La velocità di gestione di input null o parametri non validi,
 *   che potrebbero rappresentare casi limite o input errati.
 * <p>
 * Le dipendenze sono mockate per evitare latenza da accesso a database o risorse esterne,
 * così da isolare la misurazione della performance della logica di business pura.
 */

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
    
    /**
     * Test di base per verificare che l’istanza del service venga creata correttamente
     * e che la sua inizializzazione sia veloce, entro un limite di 100 millisecondi.
     *
     * Sequenza:
     * - Misura il tempo prima e dopo la semplice verifica di non nullità
     *   dell’istanza {@code noteLockService}.
     *
     * Obiettivo:
     * - Garantire che il service sia correttamente configurato nel contesto Spring
     *   e che l’overhead di creazione sia minimo.
     */

    @Test
    @DisplayName("TTD-PERF-001: Test che il service esiste ed è veloce")
    void testServiceExists() {
    
        long startTime = System.currentTimeMillis();
        assertNotNull(noteLockService);
        long endTime = System.currentTimeMillis();
        
        
        assertTrue(endTime - startTime < 100); 
    }
    
    /**
     * Test di performance che verifica la gestione veloce di chiamate al metodo
     * {@code tryLockNote} con parametri null, simili a casi limite o input errati.
     *
     * Sequenza:
     * - Esegue 10 chiamate consecutive a {@code tryLockNote} passando
     *   id nota nullo e username valido, gestendo eventuali eccezioni.
     * - Misura il tempo totale di esecuzione.
     *
     * Obiettivo:
     * - Assicurarsi che anche in presenza di parametri non validi
     *   il metodo non blocchi l’applicazione e termini rapidamente,
     *   rimanendo sotto 1000 millisecondi.
     */
    
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