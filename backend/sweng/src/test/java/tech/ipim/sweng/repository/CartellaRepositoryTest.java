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

    @Test
    void shouldFindCartelleByProprietario() {
        List<Cartella> cartelle = cartellaRepository.findByProprietarioOrderByDataModificaDesc(testUser1);

        assertThat(cartelle).hasSize(2);
        assertThat(cartelle).extracting(Cartella::getNome)
                .containsExactly("Personale", "Lavoro");
    }

    @Test
    void shouldFindCartellaByNomeAndProprietario() {
 
        Optional<Cartella> foundCartella = cartellaRepository.findByNomeAndProprietario("Lavoro", testUser1);


        assertThat(foundCartella).isPresent();
        assertThat(foundCartella.get().getNome()).isEqualTo("Lavoro");
        assertThat(foundCartella.get().getDescrizione()).isEqualTo("Cartella per note di lavoro");
        assertThat(foundCartella.get().getColore()).isEqualTo("#ff6b6b");
    }

    @Test
    void shouldNotFindCartellaByNomeAndProprietarioWhenNotExists() {
  
        Optional<Cartella> foundCartella = cartellaRepository.findByNomeAndProprietario("NonEsiste", testUser1);

 
        assertThat(foundCartella).isEmpty();
    }

    @Test
    void shouldNotFindCartellaOfOtherUser() {

        Optional<Cartella> foundCartella = cartellaRepository.findByNomeAndProprietario("Studio", testUser1);


        assertThat(foundCartella).isEmpty();
    }

    @Test
    void shouldCheckExistsByNomeAndProprietario() {
  
        boolean exists = cartellaRepository.existsByNomeAndProprietario("Lavoro", testUser1);
        boolean notExists = cartellaRepository.existsByNomeAndProprietario("NonEsiste", testUser1);


        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldCountCartelleByProprietario() {

        long count = cartellaRepository.countByProprietario(testUser1);


        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldFindCartelleByUsername() {

        List<Cartella> cartelle = cartellaRepository.findByUsername("testuser1");


        assertThat(cartelle).hasSize(2);
        assertThat(cartelle).extracting(Cartella::getNome)
                .containsExactlyInAnyOrder("Lavoro", "Personale");
    }

    @Test
    void shouldFindCartellaByIdAndUsername() {

        Optional<Cartella> foundCartella = cartellaRepository.findByIdAndUsername(cartellaLavoro.getId(), "testuser1");


        assertThat(foundCartella).isPresent();
        assertThat(foundCartella.get().getNome()).isEqualTo("Lavoro");
    }

    @Test
    void shouldNotFindCartellaByIdAndUsernameForWrongUser() {
 
        Optional<Cartella> foundCartella = cartellaRepository.findByIdAndUsername(cartellaLavoro.getId(), "testuser2");

 
        assertThat(foundCartella).isEmpty();
    }

    @Test
    void shouldNotFindCartellaByWrongId() {

        Optional<Cartella> foundCartella = cartellaRepository.findByIdAndUsername(999L, "testuser1");


        assertThat(foundCartella).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForUserWithoutCartelle() {
        
        User userSenzaCartelle = new User("usersenza", "password");
        userSenzaCartelle = entityManager.persistAndFlush(userSenzaCartelle);

    
        List<Cartella> cartelle = cartellaRepository.findByProprietarioOrderByDataModificaDesc(userSenzaCartelle);


        assertThat(cartelle).isEmpty();
    }

    @Test
    void shouldHandleCaseInsensitiveSearch() {
       
        Optional<Cartella> foundCartella = cartellaRepository.findByNomeAndProprietario("LAVORO", testUser1);

        
        assertThat(foundCartella).isEmpty();
    }

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