package tech.ipim.sweng.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByAutoreOrderByDataModificaDesc(User autore);

    @Query("SELECT n FROM Note n WHERE " +
            "n.autore.username = :username OR " +
            "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR " +
            "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura) " +
            "ORDER BY n.dataModifica DESC")
    List<Note> findAllAccessibleNotes(@Param("username") String username);

    @Query("SELECT n FROM Note n WHERE " +
            "(n.autore.username = :username OR " +
            "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR " +
            "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) " +
            "AND n.id = :noteId")
    Optional<Note> findAccessibleNoteById(@Param("noteId") Long noteId, @Param("username") String username);

    @Query("SELECT n FROM Note n WHERE " +
            "(n.autore.username = :username OR " +
            "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR " +
            "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) " +
            "AND (LOWER(n.titolo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(n.contenuto) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Note> searchNotesByKeyword(@Param("username") String username, @Param("keyword") String keyword);

    @Query("SELECT n FROM Note n JOIN n.tags t WHERE " +
            "(n.autore.username = :username OR " +
            "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR " +
            "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) " +
            "AND t = :tag")
    List<Note> findNotesByTag(@Param("username") String username, @Param("tag") String tag);

    @Query("SELECT n FROM Note n JOIN n.cartelle c WHERE " +
            "(n.autore.username = :username OR " +
            "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR " +
            "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) " +
            "AND c = :cartella")
    List<Note> findNotesByCartella(@Param("username") String username, @Param("cartella") String cartella);

    @Query("SELECT n FROM Note n WHERE " +
            "(n.autore.username = :username OR " +
            "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR " +
            "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) " +
            "AND n.autore.username = :autore")
    List<Note> findNotesByAutore(@Param("username") String username, @Param("autore") String autore);

    @Query("SELECT n FROM Note n WHERE " +
            "(n.autore.username = :username OR " +
            "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR " +
            "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) " +
            "AND DATE(n.dataCreazione) = DATE(:dataCreazione)")
    List<Note> findNotesByDataCreazione(@Param("username") String username, @Param("dataCreazione") LocalDateTime dataCreazione);

    @Query("SELECT n FROM Note n WHERE " +
            "(n.autore.username = :username OR " +
            "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR " +
            "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) " +
            "AND DATE(n.dataModifica) = DATE(:dataModifica)")
    List<Note> findNotesByDataModifica(@Param("username") String username, @Param("dataModifica") LocalDateTime dataModifica);

    @Query("SELECT n FROM Note n WHERE n.isLockedForEditing = true AND n.lockExpiresAt < :currentTime")
    List<Note> findExpiredLocks(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT COUNT(n) FROM Note n WHERE n.autore.username = :username")
    long countNotesByAutore(@Param("username") String username);

    @Query("SELECT COUNT(n) FROM Note n WHERE " +
            "n.tipoPermesso IN ('CONDIVISA_LETTURA', 'CONDIVISA_SCRITTURA') AND " +
            "(:username MEMBER OF n.permessiLettura OR :username MEMBER OF n.permessiScrittura)")
    long countSharedNotesForUser(@Param("username") String username);

    @Query("SELECT DISTINCT t FROM Note n JOIN n.tags t WHERE n.autore.username = :username")
    List<String> findAllTagsByAutore(@Param("username") String username);

    @Query("SELECT DISTINCT c FROM Note n JOIN n.cartelle c WHERE n.autore.username = :username")
    List<String> findAllCartelleByAutore(@Param("username") String username);

    @Query("SELECT DISTINCT t FROM Note n JOIN n.tags t WHERE " +
            "(n.autore.username = :username OR " +
            "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR " +
            "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura))")
    List<String> findAllTagsByUser(@Param("username") String username);

    @Query("SELECT DISTINCT c FROM Note n JOIN n.cartelle c WHERE " +
            "(n.autore.username = :username OR " +
            "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR " +
            "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura))")
    List<String> findAllCartelleByUser(@Param("username") String username);

    @Query("SELECT COUNT(n) FROM Note n WHERE " +
            "n.tipoPermesso IN ('CONDIVISA_LETTURA', 'CONDIVISA_SCRITTURA') AND " +
            "(:username MEMBER OF n.permessiLettura OR :username MEMBER OF n.permessiScrittura)")
    long countSharedNotes(@Param("username") String username);

    @Query("SELECT COUNT(n) FROM Note n WHERE n.autore = :autore")
    long countByAutore(@Param("autore") User autore);
}