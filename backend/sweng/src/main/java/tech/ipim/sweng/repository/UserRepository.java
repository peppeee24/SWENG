package tech.ipim.sweng.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.ipim.sweng.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Trova un utente per username
     * @param username l'username da cercare
     * @return Optional contenente l'utente se trovato
     */
    Optional<User> findByUsername(String username);

    /**
     * Verifica se esiste un utente con lo username specificato
     * @param username l'username da verificare
     * @return true se esiste, false altrimenti
     */
    boolean existsByUsername(String username);
}