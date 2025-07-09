package tech.ipim.sweng.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.User;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * Trova tutte le note create da un autore ordinate per data modifica decrescente
     */
    List<Note> findByAutoreOrderByDataModificaDesc(User autore);

    /**
     * Trova tutte le note accessibili all'utente (autore o con permessi di lettura o scrittura)
     */
    @Query("SELECT n FROM Note n WHERE "
            + "n.autore.username = :username OR "
            + "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR "
            + "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura) "
            + "ORDER BY n.dataModifica DESC")
    List<Note> findAllAccessibleNotes(@Param("username") String username);

    /**
     * Trova una nota specifica accessibile all'utente dato l'id della nota
     */
    @Query("SELECT n FROM Note n WHERE "
            + "(n.autore.username = :username OR "
            + "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR "
            + "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) "
            + "AND n.id = :noteId")
    Optional<Note> findAccessibleNoteById(@Param("noteId") Long noteId, @Param("username") String username);

    /**
     * Cerca note accessibili all'utente con parole chiave nel titolo o contenuto (case insensitive)
     */
    @Query("SELECT n FROM Note n WHERE "
            + "(n.autore.username = :username OR "
            + "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR "
            + "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) "
            + "AND (LOWER(n.titolo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
            + "LOWER(n.contenuto) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Note> searchNotesByKeyword(@Param("username") String username, @Param("keyword") String keyword);

    /**
     * Trova note accessibili all'utente filtrate per tag specifico
     */
    @Query("SELECT n FROM Note n JOIN n.tags t WHERE "
            + "(n.autore.username = :username OR "
            + "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR "
            + "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) "
            + "AND t = :tag")
    List<Note> findNotesByTag(@Param("username") String username, @Param("tag") String tag);

    /**
     * Trova note accessibili all'utente filtrate per cartella specifica
     */
    @Query("SELECT n FROM Note n JOIN n.cartelle c WHERE "
            + "(n.autore.username = :username OR "
            + "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR "
            + "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) "
            + "AND c = :cartella")
    List<Note> findNotesByCartella(@Param("username") String username, @Param("cartella") String cartella);

    /**
     * Trova note accessibili all'utente create da un autore specifico
     */
    @Query("SELECT n FROM Note n WHERE "
            + "(n.autore.username = :username OR "
            + "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR "
            + "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) "
            + "AND n.autore.username = :autore")
    List<Note> findNotesByAutore(@Param("username") String username, @Param("autore") String autore);

    /**
     * Trova note accessibili all'utente create in un intervallo di date (dataCreazione)
     */
    @Query("SELECT n FROM Note n WHERE "
            + "(n.autore.username = :username OR "
            + "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR "
            + "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) "
            + "AND n.dataCreazione >= :dataInizio AND n.dataCreazione <= :dataFine")
    List<Note> findNotesByDataCreazione(@Param("username") String username, @Param("dataInizio") LocalDateTime dataInizio, @Param("dataFine") LocalDateTime dataFine);

    /**
     * Trova note accessibili all'utente modificate in un intervallo di date (dataModifica)
     */
    @Query("SELECT n FROM Note n WHERE "
            + "(n.autore.username = :username OR "
            + "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR "
            + "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura)) "
            + "AND n.dataModifica >= :dataInizio AND n.dataModifica <= :dataFine")
    List<Note> findNotesByDataModifica(@Param("username") String username, @Param("dataInizio") LocalDateTime dataInizio, @Param("dataFine") LocalDateTime dataFine);

    /**
     * Trova note bloccate per modifica che hanno il blocco scaduto
     */
    @Query("SELECT n FROM Note n WHERE n.isLockedForEditing = true AND n.lockExpiresAt < :currentTime")
    List<Note> findExpiredLocks(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Conta il numero di note create da un dato autore
     */
    @Query("SELECT COUNT(n) FROM Note n WHERE n.autore.username = :username")
    long countNotesByAutore(@Param("username") String username);

    /**
     * Conta il numero di note condivise (lettura o scrittura) per un dato utente
     */
    @Query("SELECT COUNT(n) FROM Note n WHERE "
            + "n.tipoPermesso IN ('CONDIVISA_LETTURA', 'CONDIVISA_SCRITTURA') AND "
            + "(:username MEMBER OF n.permessiLettura OR :username MEMBER OF n.permessiScrittura)")
    long countSharedNotesForUser(@Param("username") String username);

    /**
     * Trova tutti i tag distinti utilizzati dall'autore
     */
    @Query("SELECT DISTINCT t FROM Note n JOIN n.tags t WHERE n.autore.username = :username")
    List<String> findAllTagsByAutore(@Param("username") String username);

    /**
     * Trova tutte le cartelle distinte utilizzate dall'autore
     */
    @Query("SELECT DISTINCT c FROM Note n JOIN n.cartelle c WHERE n.autore.username = :username")
    List<String> findAllCartelleByAutore(@Param("username") String username);

    /**
     * Trova tutti i tag distinti accessibili all'utente
     */
    @Query("SELECT DISTINCT t FROM Note n JOIN n.tags t WHERE "
            + "(n.autore.username = :username OR "
            + "(n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura) OR "
            + "(n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND :username MEMBER OF n.permessiScrittura))")
    List<String> findAllTagsByUser(@Param("username") String username);

    /**
     * Trova tutte le cartelle distinte accessibili all'utente
     */
    @Query("SELECT DISTINCT c FROM Note n JOIN n.cartelle c WHERE n.autore.username = :username")
    List<String> findAllCartelleByUser(@Param("username") String username);

    /**
     * Conta il numero di note condivise accessibili per un dato utente
     */
    @Query("SELECT COUNT(n) FROM Note n WHERE "
            + "n.tipoPermesso IN ('CONDIVISA_LETTURA', 'CONDIVISA_SCRITTURA') AND "
            + "(:username MEMBER OF n.permessiLettura OR :username MEMBER OF n.permessiScrittura)")
    long countSharedNotes(@Param("username") String username);

    /**
     * Conta il numero di note create da un dato autore (usando l'entità User)
     */
    @Query("SELECT COUNT(n) FROM Note n WHERE n.autore = :autore")
    long countByAutore(@Param("autore") User autore);

    /**
     * Trova tutti gli autori distinti accessibili all'utente
     */
    @Query("SELECT DISTINCT n.autore.username FROM Note n "
            + "WHERE n.autore.username = :username "
            + "OR n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura "
            + "OR n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND (:username MEMBER OF n.permessiLettura OR :username MEMBER OF n.permessiScrittura) "
            + "ORDER BY n.autore.username")
    List<String> findDistinctAutoriByAccessibleToUser(@Param("username") String username);

    /**
     * Trova note proprie dell'autore filtrate per username e ordinate per data modifica decrescente
     */
    @Query("SELECT n FROM Note n WHERE n.autore.username = :autore AND n.autore.username = :currentUser "
            + "ORDER BY n.dataModifica DESC")
    List<Note> findByAutoreUsernameAndAutoreUsernameOrderByDataModificaDesc(
            @Param("autore") String autore,
            @Param("currentUser") String currentUser);

    /**
     * Trova note condivise filtrate per autore e accessibili all'utente
     */
    @Query("SELECT n FROM Note n WHERE n.autore.username = :autore "
            + "AND (n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura "
            + "OR n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND (:username MEMBER OF n.permessiLettura OR :username MEMBER OF n.permessiScrittura)) "
            + "ORDER BY n.dataModifica DESC")
    List<Note> findSharedNotesByAutore(@Param("username") String username, @Param("autore") String autore);

    /**
     * Trova tutte le note accessibili filtrate per autore e ordinate per data modifica decrescente
     */
    @Query("SELECT n FROM Note n WHERE n.autore.username = :autore "
            + "AND (n.autore.username = :username "
            + "OR n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura "
            + "OR n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND (:username MEMBER OF n.permessiLettura OR :username MEMBER OF n.permessiScrittura)) "
            + "ORDER BY n.dataModifica DESC")
    List<Note> findAccessibleNotesByAutore(@Param("username") String username, @Param("autore") String autore);

    /**
     * Trova note proprie di un utente nel range di date specificato, ordinate per data modifica decrescente
     */
    @Query("SELECT n FROM Note n WHERE n.autore.username = :username "
            + "AND (:startDate IS NULL OR n.dataCreazione >= :startDate) "
            + "AND (:endDate IS NULL OR n.dataCreazione <= :endDate) "
            + "ORDER BY n.dataModifica DESC")
    List<Note> findOwnNotesByDateRange(
            @Param("username") String username,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Trova note condivise accessibili nel range di date specificato, ordinate per data modifica decrescente
     */
    @Query("SELECT n FROM Note n "
            + "WHERE (n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura "
            + "OR n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND (:username MEMBER OF n.permessiLettura OR :username MEMBER OF n.permessiScrittura)) "
            + "AND (:startDate IS NULL OR n.dataCreazione >= :startDate) "
            + "AND (:endDate IS NULL OR n.dataCreazione <= :endDate) "
            + "ORDER BY n.dataModifica DESC")
    List<Note> findSharedNotesByDateRange(
            @Param("username") String username,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Trova tutte le note accessibili all'utente filtrate per range di date, ordinate per data modifica decrescente
     */
    @Query("SELECT n FROM Note n "
            + "WHERE (n.autore.username = :username "
            + "OR n.tipoPermesso = 'CONDIVISA_LETTURA' AND :username MEMBER OF n.permessiLettura "
            + "OR n.tipoPermesso = 'CONDIVISA_SCRITTURA' AND (:username MEMBER OF n.permessiLettura OR :username MEMBER OF n.permessiScrittura)) "
            + "AND (:startDate IS NULL OR n.dataCreazione >= :startDate) "
            + "AND (:endDate IS NULL OR n.dataCreazione <= :endDate) "
            + "ORDER BY n.dataModifica DESC")
    List<Note> findAccessibleNotesByDateRange(
            @Param("username") String username,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Salva e sincronizza immediatamente la nota (flush esplicito)
     */
    default Note saveAndFlush(Note note) {
        Note saved = save(note);
        flush();  // Forza il salvataggio immediato
        return saved;
    }

}
