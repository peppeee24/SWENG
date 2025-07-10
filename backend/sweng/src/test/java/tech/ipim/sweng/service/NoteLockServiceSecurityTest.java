package tech.ipim.sweng.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

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

import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteLockRepository;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;

/**
 * Test di sicurezza per {@link NoteLockService} con esecuzione di metodi reali.
 * <p>
 * Questi test verificano la corretta gestione degli scenari di sicurezza più critici,
 * inclusi tentativi di accesso a note inesistenti, utenti non validi e validazione dei parametri.
 * <p>
 * L’obiettivo è garantire che il servizio rifiuti correttamente chiamate non autorizzate
 * o malformate, lanciando eccezioni appropriate per impedire azioni illegittime.
 * <p>
 * Vengono mockate le dipendenze per isolare la logica di business e simulare condizioni specifiche,
 * come la presenza o assenza di note e utenti nel sistema.
 */

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("NoteLockService Security - Test sicurezza con metodi reali")
class NoteLockServiceSecurityTest {

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
    
    private User owner;
    private Note privateNote;
    
    /**
     * Setup iniziale comune a tutti i test.
     * <p>
     * Crea un utente "owner" e una nota privata associata a tale utente.
     * La nota ha permessi di lettura e scrittura vuoti per simulare uno scenario di controllo stretto.
     * Imposta data di creazione e modifica all’istante corrente.
     */

    @BeforeEach
    void setUp() {
        
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setEmail("owner@example.com");
        
        privateNote = new Note();
        privateNote.setId(1L);
        privateNote.setTitolo("Private Note");
        privateNote.setContenuto("Private content");
        privateNote.setAutore(owner);
        privateNote.setTipoPermesso(TipoPermesso.PRIVATA);
        privateNote.setPermessiLettura(new HashSet<>());
        privateNote.setPermessiScrittura(new HashSet<>());
        privateNote.setDataCreazione(LocalDateTime.now());
        privateNote.setDataModifica(LocalDateTime.now());
    }
    
    /**
     * Verifica che il tentativo di bloccare una nota inesistente
     * generi un’eccezione di runtime.
     * <p>
     * Simula la risposta vuota del repository per una nota con ID non presente.
     */

    @Test
    @DisplayName("TTD-SECURITY-001: Test con note inesistenti")
    void testWithNonExistentNote() {
        
        when(noteRepository.findById(999L)).thenReturn(Optional.empty());
        
        
        assertThrows(RuntimeException.class, () -> {
            noteLockService.tryLockNote(999L, "owner");
        });
    }
    
     /**
     * Verifica che il tentativo di bloccare una nota da parte di un utente inesistente
     * generi un’eccezione di runtime.
     * <p>
     * Simula la presenza della nota ma l’assenza dell’utente.
     */

    @Test
    @DisplayName("TTD-SECURITY-002: Test con utenti inesistenti")
    void testWithNonExistentUser() {
        
        when(noteRepository.findById(1L)).thenReturn(Optional.of(privateNote));
        assertThrows(RuntimeException.class, () -> {
            noteLockService.tryLockNote(1L, "nonexistent");
        });
    }
    
    /**
     * Testa la validazione dei parametri di input per il metodo {@code tryLockNote}.
     * <p>
     * Verifica che vengano sollevate eccezioni se:
     * - l’id della nota è null,
     * - il nome utente è null,
     * - il nome utente è una stringa vuota.
     */
    
    @Test
    @DisplayName("TTD-SECURITY-003: Test validazione parametri null/vuoti")
    void testParameterValidation() {
        assertThrows(RuntimeException.class, () -> {
            noteLockService.tryLockNote(null, "user");
        });
        
        assertThrows(RuntimeException.class, () -> {
            noteLockService.tryLockNote(1L, null);
        });
        
        assertThrows(RuntimeException.class, () -> {
            noteLockService.tryLockNote(1L, "");
        });
    }
}