package tech.ipim.sweng.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ipim.sweng.model.Cartella;
import tech.ipim.sweng.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartellaRepository extends JpaRepository<Cartella, Long> {

    /**
     * Trova tutte le cartelle di un proprietario
     */
    List<Cartella> findByProprietarioOrderByDataModificaDesc(User proprietario);

    /**
     * Trova cartella per nome e proprietario
     */
    Optional<Cartella> findByNomeAndProprietario(String nome, User proprietario);

    /**
     * Verifica se esiste già una cartella con lo stesso nome per l'utente
     */
    boolean existsByNomeAndProprietario(String nome, User proprietario);

    /**
     * Conta le cartelle di un utente
     */
    long countByProprietario(User proprietario);

    /**
     * Trova cartelle per username del proprietario
     */
    @Query("SELECT c FROM Cartella c WHERE c.proprietario.username = :username ORDER BY c.dataModifica DESC")
    List<Cartella> findByUsername(@Param("username") String username);

    /**
     * Trova cartella per ID e username del proprietario
     */
    @Query("SELECT c FROM Cartella c WHERE c.id = :id AND c.proprietario.username = :username")
    Optional<Cartella> findByIdAndUsername(@Param("id") Long id, @Param("username") String username);
}