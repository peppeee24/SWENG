package tech.ipim.sweng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteLockServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @InjectMocks
    private NoteLockService noteLockService;

    private User autore;
    private User altroUtente;
    private Note nota;

    @BeforeEach
    void setUp() {
        autore = new User();
        autore.setUsername("autore");

        altroUtente = new User();
        altroUtente.setUsername("altro_utente");

        nota = new Note();
        nota.setId(1L);
        nota.setTitolo("Test Note");
        nota.setContenuto("Contenuto di test");
        nota.setAutore(autore);
        nota.setTipoPermesso(TipoPermesso.CONDIVISA_SCRITTURA);
        Set<String> permessiScrittura = new HashSet<>();
        permessiScrittura.add("altro_utente");
        nota.setPermessiScrittura(permessiScrittura);
        nota.setIsLockedForEditing(false);
    }

    @Test
    void testAcquireLock_Success() {
        when(noteRepository.findById(1L)).thenReturn(Optional.of(nota));
        when(noteRepository.save(any(Note.class))).thenReturn(nota);

        NoteLockService.LockResult result = noteLockService.acquireLock(1L, "autore");

        assertTrue(result.isSuccess());
        assertEquals("Lock acquisito con successo", result.getMessage());
        assertEquals("autore", result.getLockedByUser());
        assertNotNull(result.getLockExpiresAt());
        verify(noteRepository).save(nota);
    }

    @Test
    void testAcquireLock_NoteNotFound() {
        when(noteRepository.findById(1L)).thenReturn(Optional.empty());

        NoteLockService.LockResult result = noteLockService.acquireLock(1L, "autore");

        assertFalse(result.isSuccess());
        assertEquals("Nota non trovata", result.getMessage());
        verify(noteRepository, never()).save(any());
    }

    @Test
    void testAcquireLock_InsufficientPermissions() {
        nota.setTipoPermesso(TipoPermesso.PRIVATA);
        nota.setPermessiScrittura(new HashSet<>());

        when(noteRepository.findById(1L)).thenReturn(Optional.of(nota));

        NoteLockService.LockResult result = noteLockService.acquireLock(1L, "altro_utente");

        assertFalse(result.isSuccess());
        assertEquals("Permessi insufficienti per modificare questa nota", result.getMessage());
        verify(noteRepository, never()).save(any());
    }

    @Test
    void testAcquireLock_AlreadyLockedByAnotherUser() {
        nota.setIsLockedForEditing(true);
        nota.setLockedByUser("altro_utente");
        nota.setLockExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(noteRepository.findById(1L)).thenReturn(Optional.of(nota));

        NoteLockService.LockResult result = noteLockService.acquireLock(1L, "autore");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Nota attualmente in modifica da altro_utente"));
        assertEquals("altro_utente", result.getLockedByUser());
        verify(noteRepository, never()).save(any());
    }

    @Test
    void testAcquireLock_AlreadyLockedBySameUser() {
        nota.setIsLockedForEditing(true);
        nota.setLockedByUser("autore");
        LocalDateTime lockExpiration = LocalDateTime.now().plusMinutes(5);
        nota.setLockExpiresAt(lockExpiration);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(nota));

        NoteLockService.LockResult result = noteLockService.acquireLock(1L, "autore");

        assertTrue(result.isSuccess());
        assertEquals("Lock già acquisito dall'utente", result.getMessage());
        assertEquals("autore", result.getLockedByUser());
        assertEquals(lockExpiration, result.getLockExpiresAt());
        verify(noteRepository, never()).save(any());
    }

    @Test
    void testAcquireLock_ExpiredLock() {
        nota.setIsLockedForEditing(true);
        nota.setLockedByUser("altro_utente");
        nota.setLockExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(noteRepository.findById(1L)).thenReturn(Optional.of(nota));
        when(noteRepository.save(any(Note.class))).thenReturn(nota);

        NoteLockService.LockResult result = noteLockService.acquireLock(1L, "autore");

        assertTrue(result.isSuccess());
        assertEquals("Lock acquisito con successo", result.getMessage());
        assertEquals("autore", result.getLockedByUser());
        verify(noteRepository).save(nota);
    }

    @Test
    void testReleaseLock_Success() {
        nota.setIsLockedForEditing(true);
        nota.setLockedByUser("autore");
        nota.setLockExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(noteRepository.findById(1L)).thenReturn(Optional.of(nota));
        when(noteRepository.save(any(Note.class))).thenReturn(nota);

        NoteLockService.LockResult result = noteLockService.releaseLock(1L, "autore");

        assertTrue(result.isSuccess());
        assertEquals("Lock rilasciato con successo", result.getMessage());
        assertNull(result.getLockedByUser());
        verify(noteRepository).save(nota);
    }

    @Test
    void testReleaseLock_NotLocked() {
        nota.setIsLockedForEditing(false);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(nota));

        NoteLockService.LockResult result = noteLockService.releaseLock(1L, "autore");

        assertTrue(result.isSuccess());
        assertEquals("Nota non era bloccata", result.getMessage());
        verify(noteRepository, never()).save(any());
    }

    @Test
    void testReleaseLock_WrongUser() {
        nota.setIsLockedForEditing(true);
        nota.setLockedByUser("altro_utente");
        nota.setLockExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(noteRepository.findById(1L)).thenReturn(Optional.of(nota));

        NoteLockService.LockResult result = noteLockService.releaseLock(1L, "autore");

        assertFalse(result.isSuccess());
        assertEquals("Non puoi rilasciare un lock di un altro utente", result.getMessage());
        verify(noteRepository, never()).save(any());
    }

    @Test
    void testGetLockStatus_Locked() {
        nota.setIsLockedForEditing(true);
        nota.setLockedByUser("autore");
        LocalDateTime lockExpiration = LocalDateTime.now().plusMinutes(5);
        nota.setLockExpiresAt(lockExpiration);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(nota));

        NoteLockService.LockStatus status = noteLockService.getLockStatus(1L);

        assertTrue(status.isLocked());
        assertEquals("autore", status.getLockedByUser());
        assertEquals(lockExpiration, status.getLockExpiresAt());
    }

    @Test
    void testGetLockStatus_NotLocked() {
        nota.setIsLockedForEditing(false);

        when(noteRepository.findById(1L)).thenReturn(Optional.of(nota));

        NoteLockService.LockStatus status = noteLockService.getLockStatus(1L);

        assertFalse(status.isLocked());
        assertNull(status.getLockedByUser());
        assertNull(status.getLockExpiresAt());
    }

    @Test
    void testGetLockStatus_ExpiredLock() {
        nota.setIsLockedForEditing(true);
        nota.setLockedByUser("autore");
        nota.setLockExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(noteRepository.findById(1L)).thenReturn(Optional.of(nota));

        NoteLockService.LockStatus status = noteLockService.getLockStatus(1L);

        assertFalse(status.isLocked());
        assertNull(status.getLockedByUser());
        assertNull(status.getLockExpiresAt());
    }
}