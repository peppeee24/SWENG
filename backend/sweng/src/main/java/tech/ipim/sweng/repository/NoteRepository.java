package tech.ipim.sweng.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * Trova tutte le note create da un autore specifico
     */
    List<Note> findByAutoreOrderByDataModificaDesc(User autore);

    /**
     * Trova tutte le note accessibili a un utente (proprie + condivise)
     */
    @Query("SELECT n FROM Note n WHERE " +
           "n.autore.username = :username OR " +
           "(:username MEMBER OF n.permessiLettura) OR " +
           "(:username MEMBER OF n.permessiScrittura) " +
           "ORDER BY n.dataModifica DESC")
    List<Note> findAllAccessibleNotes(@Param("username") String username);

    /**
     * Trova una nota per ID se l'utente ha accesso
     */
    @Query("SELECT n FROM Note n WHERE n.id = :noteId AND (" +
           "n.autore.username = :username OR " +
           "(:username MEMBER OF n.permessiLettura) OR " +
           "(:username MEMBER OF n.permessiScrittura))")
    Optional<Note> findAccessibleNoteById(@Param("noteId") Long noteId, @Param("username") String username);

    /**
     * Cerca note per parole chiave nel titolo o contenuto
     */
    @Query("SELECT n FROM Note n WHERE " +
           "(n.autore.username = :username OR " +
           "(:username MEMBER OF n.permessiLettura) OR " +
           "(:username MEMBER OF n.permessiScrittura)) AND " +
           "(LOWER(n.titolo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.contenuto) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY n.dataModifica DESC")
    List<Note> searchNotesByKeyword(@Param("username") String username, @Param("keyword") String keyword);

    /**
     * Trova note per tag
     */
    @Query("SELECT n FROM Note n JOIN n.tags t WHERE " +
           "(n.autore.username = :username OR " +
           "(:username MEMBER OF n.permessiLettura) OR " +
           "(:username MEMBER OF n.permessiScrittura)) AND " +
           "t = :tag " +
           "ORDER BY n.dataModifica DESC")
    List<Note> findNotesByTag(@Param("username") String username, @Param("tag") String tag);

    /**
     * Trova note per cartella
     */
    @Query("SELECT n FROM Note n JOIN n.cartelle c WHERE " +
           "(n.autore.username = :username OR " +
           "(:username MEMBER OF n.permessiLettura) OR " +
           "(:username MEMBER OF n.permessiScrittura)) AND " +
           "c = :cartella " +
           "ORDER BY n.dataModifica DESC")
    List<Note> findNotesByCartella(@Param("username") String username, @Param("cartella") String cartella);

    /**
     * Conta le note create da un autore
     */
    long countByAutore(User autore);

    /**
     * Trova note condivise con un utente specifico
     */
    @Query("SELECT n FROM Note n WHERE " +
           "(:username MEMBER OF n.permessiLettura) OR " +
           "(:username MEMBER OF n.permessiScrittura) " +
           "ORDER BY n.dataModifica DESC")
    List<Note> findSharedNotes(@Param("username") String username);

    /**
     * Conta note condivise con un utente
     */
    @Query("SELECT COUNT(n) FROM Note n WHERE " +
           "(:username MEMBER OF n.permessiLettura) OR " +
           "(:username MEMBER OF n.permessiScrittura)")
    long countSharedNotes(@Param("username") String username);

    /**
     * Trova tutti i tag utilizzati dall'utente
     */
    @Query("SELECT DISTINCT t FROM Note n JOIN n.tags t WHERE " +
           "n.autore.username = :username OR " +
           "(:username MEMBER OF n.permessiLettura) OR " +
           "(:username MEMBER OF n.permessiScrittura) " +
           "ORDER BY t")
    List<String> findAllTagsByUser(@Param("username") String username);

    /**
     * Trova tutte le cartelle utilizzate dall'utente
     */
    @Query("SELECT DISTINCT c FROM Note n JOIN n.cartelle c WHERE " +
           "n.autore.username = :username OR " +
           "(:username MEMBER OF n.permessiLettura) OR " +
           "(:username MEMBER OF n.permessiScrittura) " +
           "ORDER BY c")
    List<String> findAllCartelleByUser(@Param("username") String username);
}