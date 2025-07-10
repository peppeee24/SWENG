package tech.ipim.sweng.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ipim.sweng.model.NoteVersion;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteVersionRepository extends JpaRepository<NoteVersion, Long> {

    /**
     * Trova tutte le versioni di una nota ordinate dal numero di versione più alto al più basso
     */
    List<NoteVersion> findByNoteIdOrderByVersionNumberDesc(Long noteId);

    /**
     * Trova il numero dell'ultima versione di una nota (massimo versionNumber)
     */
    @Query("SELECT MAX(nv.versionNumber) FROM NoteVersion nv WHERE nv.note.id = :noteId")
    Optional<Integer> findLatestVersionNumber(@Param("noteId") Long noteId);

    /**
     * Trova una versione specifica di una nota dato il suo id e il numero della versione
     */
    Optional<NoteVersion> findByNoteIdAndVersionNumber(Long noteId, Integer versionNumber);

    /**
     * Trova tutta la cronologia delle versioni di una nota ordinate dal numero di versione più alto al più basso
     */
    @Query("SELECT nv FROM NoteVersion nv WHERE nv.note.id = :noteId ORDER BY nv.versionNumber DESC")
    List<NoteVersion> findVersionHistory(@Param("noteId") Long noteId);
}
