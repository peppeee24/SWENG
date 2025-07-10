package tech.ipim.sweng.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import tech.ipim.sweng.model.Cartella;
import tech.ipim.sweng.model.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test di integrazione per il repository {@link CartellaRepository}, responsabile della gestione
 * delle operazioni CRUD e query personalizzate sulle entità {@link Cartella}.
 * <p>
 * Verifica il corretto funzionamento dei metodi di ricerca, esistenza, conteggio e ordinamento
 * delle cartelle associate agli utenti.
 * <p>
 * <p>
 * Riepilogo dei test implementati:
 * <ul>
 *   <li>{@code shouldFindCartelleByProprietario} – Recupera le cartelle di un utente ordinandole per data di modifica decrescente</li>
 *   <li>{@code shouldFindCartellaByNomeAndProprietario} – Recupera una cartella specifica per nome e proprietario</li>
 *   <li>{@code shouldNotFindCartellaByNomeAndProprietarioWhenNotExists} – Verifica il comportamento con nome inesistente</li>
 *   <li>{@code shouldNotFindCartellaOfOtherUser} – Assicura che un utente non possa accedere alle cartelle di altri</li>
 *   <li>{@code shouldCheckExistsByNomeAndProprietario} – Verifica l’esistenza di una cartella per nome e proprietario</li>
 *   <li>{@code shouldCountCartelleByProprietario} – Conta le cartelle di un determinato proprietario</li>
 *   <li>{@code shouldFindCartelleByUsername} – Recupera le cartelle tramite username del proprietario</li>
 *   <li>{@code shouldFindCartellaByIdAndUsername} – Recupera una cartella tramite ID e username</li>
 *   <li>{@code shouldNotFindCartellaByIdAndUsernameForWrongUser} – Verifica che un altro utente non acceda a cartelle altrui</li>
 *   <li>{@code shouldNotFindCartellaByWrongId} – Testa il recupero di una cartella con ID inesistente</li>
 *   <li>{@code shouldReturnEmptyListForUserWithoutCartelle} – Gestisce il caso di utente senza cartelle associate</li>
 *   <li>{@code shouldHandleCaseInsensitiveSearch} – Verifica la sensibilità al maiuscolo/minuscolo nelle query</li>
 *   <li>{@code shouldOrderCartelleByDataModificaDesc} – Verifica l’ordinamento delle cartelle per data di modifica</li>
 * </ul>
 */

@DataJpaTest
class CartellaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartellaRepository cartellaRepository;

    private User testUser1;
    private User testUser2;
    private Cartella cartellaLavoro;
    private Cartella cartellaPersonale;

    /**
     * Inizializza i dati di test prima di ogni metodo, creando utenti e cartelle
     * persistite nel database H2 in memoria per i test di integrazione.
     */

    @BeforeEach
    void setUp() {
        testUser1 = new User("testuser1", "password123");
        testUser1.setEmail("test1@example.com");
        testUser1 = entityManager.persistAndFlush(testUser1);

        testUser2 = new User("testuser2", "password456");
        testUser2.setEmail("test2@example.com");
        testUser2 = entityManager.persistAndFlush(testUser2);

        cartellaLavoro = new Cartella("Lavoro", testUser1);
        cartellaLavoro.setDescrizione("Cartella per note di lavoro");
        cartellaLavoro.setColore("#ff6b6b");
        cartellaLavoro = entityManager.persistAndFlush(cartellaLavoro);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        cartellaPersonale = new Cartella("Personale", testUser1);
        cartellaPersonale.setDescrizione("Cartella per note personali");
        cartellaPersonale.setColore("#4ecdc4");
        cartellaPersonale = entityManager.persistAndFlush(cartellaPersonale);

        Cartella cartellaAltroUtente = new Cartella("Studio", testUser2);
        cartellaAltroUtente.setDescrizione("Cartella per appunti di studio");
        entityManager.persistAndFlush(cartellaAltroUtente);

        entityManager.clear();
    }

    /**
     * Verifica il recupero delle cartelle di un utente specifico ordinate
     * per data di modifica in ordine decrescente.
     */

    @Test
    void shouldFindCartelleByProprietario() {
        List<Cartella> cartelle = cartellaRepository.findByProprietarioOrderByDataModificaDesc(testUser1);

        assertThat(cartelle).hasSize(2);
        assertThat(cartelle).extracting(Cartella::getNome)
                .containsExactly("Personale", "Lavoro");
    }

    /**
     * Verifica il recupero di una cartella per nome e proprietario.
     */

    @Test
    void shouldFindCartellaByNomeAndProprietario() {
 
        Optional<Cartella> foundCartella = cartellaRepository.findByNomeAndProprietario("Lavoro", testUser1);


        assertThat(foundCartella).isPresent();
        assertThat(foundCartella.get().getNome()).isEqualTo("Lavoro");
        assertThat(foundCartella.get().getDescrizione()).isEqualTo("Cartella per note di lavoro");
        assertThat(foundCartella.get().getColore()).isEqualTo("#ff6b6b");
    }

     /**
     * Verifica che una ricerca per nome e proprietario ritorni vuoto se il nome non esiste.
     */

    @Test
    void shouldNotFindCartellaByNomeAndProprietarioWhenNotExists() {
  
        Optional<Cartella> foundCartella = cartellaRepository.findByNomeAndProprietario("NonEsiste", testUser1);

 
        assertThat(foundCartella).isEmpty();
    }

    /**
     * Verifica che un utente non possa accedere alle cartelle di un altro utente.
     */

    @Test
    void shouldNotFindCartellaOfOtherUser() {

        Optional<Cartella> foundCartella = cartellaRepository.findByNomeAndProprietario("Studio", testUser1);


        assertThat(foundCartella).isEmpty();
    }

    /**
     * Verifica la presenza di una cartella per nome e proprietario e il comportamento
     * in caso di nome inesistente.
     */

    @Test
    void shouldCheckExistsByNomeAndProprietario() {
  
        boolean exists = cartellaRepository.existsByNomeAndProprietario("Lavoro", testUser1);
        boolean notExists = cartellaRepository.existsByNomeAndProprietario("NonEsiste", testUser1);


        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    /**
     * Verifica il conteggio delle cartelle associate a un utente specifico.
     */

    @Test
    void shouldCountCartelleByProprietario() {

        long count = cartellaRepository.countByProprietario(testUser1);


        assertThat(count).isEqualTo(2);
    }

    /**
     * Verifica il recupero delle cartelle tramite username del proprietario.
     */

    @Test
    void shouldFindCartelleByUsername() {

        List<Cartella> cartelle = cartellaRepository.findByUsername("testuser1");


        assertThat(cartelle).hasSize(2);
        assertThat(cartelle).extracting(Cartella::getNome)
                .containsExactlyInAnyOrder("Lavoro", "Personale");
    }

    /**
     * Verifica il recupero di una cartella tramite ID e username del proprietario.
     */

    @Test
    void shouldFindCartellaByIdAndUsername() {

        Optional<Cartella> foundCartella = cartellaRepository.findByIdAndUsername(cartellaLavoro.getId(), "testuser1");


        assertThat(foundCartella).isPresent();
        assertThat(foundCartella.get().getNome()).isEqualTo("Lavoro");
    }
    
    /**
     * Verifica che un altro utente non possa accedere a una cartella per ID e username.
     */
    
    @Test
    void shouldNotFindCartellaByIdAndUsernameForWrongUser() {
 
        Optional<Cartella> foundCartella = cartellaRepository.findByIdAndUsername(cartellaLavoro.getId(), "testuser2");

 
        assertThat(foundCartella).isEmpty();
    }

    /**
     * Verifica che una ricerca con ID inesistente ritorni vuoto.
     */
    @Test
    void shouldNotFindCartellaByWrongId() {

        Optional<Cartella> foundCartella = cartellaRepository.findByIdAndUsername(999L, "testuser1");


        assertThat(foundCartella).isEmpty();
    }

    /**
     * Verifica il comportamento del repository per un utente senza cartelle associate.
     */

    @Test
    void shouldReturnEmptyListForUserWithoutCartelle() {
        
        User userSenzaCartelle = new User("usersenza", "password");
        userSenzaCartelle = entityManager.persistAndFlush(userSenzaCartelle);

    
        List<Cartella> cartelle = cartellaRepository.findByProprietarioOrderByDataModificaDesc(userSenzaCartelle);


        assertThat(cartelle).isEmpty();
    }

    /**
     * Verifica la sensibilità al maiuscolo/minuscolo nelle query per nome.
     */

    @Test
    void shouldHandleCaseInsensitiveSearch() {
       
        Optional<Cartella> foundCartella = cartellaRepository.findByNomeAndProprietario("LAVORO", testUser1);

        
        assertThat(foundCartella).isEmpty();
    }

    /**
     * Verifica l’ordinamento delle cartelle per data di modifica decrescente.
     */
    @Test
    void shouldOrderCartelleByDataModificaDesc() {

        Cartella cartellaToUpdate = entityManager.find(Cartella.class, cartellaLavoro.getId());
        cartellaToUpdate.setDescrizione("Descrizione aggiornata");
 
        cartellaToUpdate.setDataModifica(cartellaToUpdate.getDataModifica().plusMinutes(1));
        entityManager.flush();
        entityManager.clear();


        List<Cartella> cartelle = cartellaRepository.findByProprietarioOrderByDataModificaDesc(testUser1);


        assertThat(cartelle).hasSize(2);
        assertThat(cartelle.get(0).getNome()).isEqualTo("Lavoro");
        assertThat(cartelle.get(1).getNome()).isEqualTo("Personale");
    }
}