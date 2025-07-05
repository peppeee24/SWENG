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

    List<NoteVersion> findByNoteIdOrderByVersionNumberDesc(Long noteId);

    @Query("SELECT MAX(nv.versionNumber) FROM NoteVersion nv WHERE nv.note.id = :noteId")
    Optional<Integer> findLatestVersionNumber(@Param("noteId") Long noteId);

    Optional<NoteVersion> findByNoteIdAndVersionNumber(Long noteId, Integer versionNumber);

    @Query("SELECT nv FROM NoteVersion nv WHERE nv.note.id = :noteId ORDER BY nv.versionNumber DESC")
    List<NoteVersion> findVersionHistory(@Param("noteId") Long noteId);

    //  Recupera tutte le versioni di una nota
    List<NoteVersion> findByNoteId(Long noteId);

    //  Recupera una versione specifica per ID
    Optional<NoteVersion> findById(Long versionId);

}