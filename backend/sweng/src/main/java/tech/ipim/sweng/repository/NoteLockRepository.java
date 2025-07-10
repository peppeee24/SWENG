package tech.ipim.sweng.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ipim.sweng.model.NoteLock;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NoteLockRepository extends JpaRepository<NoteLock, Long> {

    /**
     * Trova il blocco della nota tramite l'ID della nota
     */
    Optional<NoteLock> findByNoteId(Long noteId);

    /**
     * Elimina tutti i blocchi scaduti (con expiresAt precedente al parametro now)
     */
    @Modifying
    @Query("DELETE FROM NoteLock nl WHERE nl.expiresAt < :now")
    void deleteExpiredLocks(@Param("now") LocalDateTime now);

    /**
     * Verifica se esiste un blocco attivo per una data nota (non scaduto)
     */
    @Query("SELECT COUNT(nl) > 0 FROM NoteLock nl WHERE nl.noteId = :noteId AND nl.expiresAt > :now")
    boolean existsActiveByNoteId(@Param("noteId") Long noteId, @Param("now") LocalDateTime now);

    /**
     * Elimina il blocco di una nota creato da uno specifico utente
     */
    @Modifying
    @Query("DELETE FROM NoteLock nl WHERE nl.noteId = :noteId AND nl.lockedBy = :username")
    void deleteByNoteIdAndLockedBy(@Param("noteId") Long noteId, @Param("username") String username);
}
