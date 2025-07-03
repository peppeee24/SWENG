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
    
    @Test
    @DisplayName("TTD-SECURITY-001: Test con note inesistenti")
    void testWithNonExistentNote() {
        
        when(noteRepository.findById(999L)).thenReturn(Optional.empty());
        
        
        assertThrows(RuntimeException.class, () -> {
            noteLockService.tryLockNote(999L, "owner");
        });
    }
    
    @Test
    @DisplayName("TTD-SECURITY-002: Test con utenti inesistenti")
    void testWithNonExistentUser() {
        
        when(noteRepository.findById(1L)).thenReturn(Optional.of(privateNote));
        assertThrows(RuntimeException.class, () -> {
            noteLockService.tryLockNote(1L, "nonexistent");
        });
    }
    
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