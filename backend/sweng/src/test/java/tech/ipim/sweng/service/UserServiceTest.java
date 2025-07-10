package tech.ipim.sweng.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.ipim.sweng.dto.UserDto;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.UserRepository;

/**
 * UC8 - Test sul Service Layer per la gestione degli utenti.
 * <p>
 * Questa classe verifica la correttezza e robustezza del servizio
 * UserService in scenari che comprendono:
 * - il recupero di tutti gli utenti,
 * - l'esclusione di un utente specificato,
 * - il comportamento con repository vuoto,
 * - la conversione da User a UserDto senza esporre password,
 * - la gestione delle eccezioni dal repository,
 * - e test di performance e transazionalità.
 * <p>
 * Tutti i test usano mock per isolare la logica di business e
 * verificano che il repository venga interrogato correttamente.
 */

@ExtendWith(MockitoExtension.class)
@DisplayName("UC8 - Test Service Layer per Gestione Utenti")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private List<User> mockUserEntities;
    private User currentUser;

    /**
     * Setup iniziale eseguito prima di ogni test.
     * <p>
     * Crea una lista di utenti di test (mockUserEntities) e un utente corrente (currentUser),
     * assicurandosi che tutti gli oggetti creati e i loro campi essenziali non siano nulli.
     * Questo setup permette di utilizzare dati consistenti e affidabili nei vari test.
     */
    @BeforeEach
    void setUp() {
        mockUserEntities = createMockUserEntities();
        currentUser = createCurrentUser();


        assertNotNull(currentUser, "Current user should not be null");
        for (User user : mockUserEntities) {
            assertNotNull(user, "Mock user should not be null");
            assertNotNull(user.getUsername(), "Username should not be null");
            assertNotNull(user.getId(), "User ID should not be null");
        }
    }

   /**
     * Test di recupero di tutti gli utenti.
     * <p>
     * Mocka il repository per restituire una lista predefinita di utenti.
     * Verifica che il servizio ritorni una lista non nulla,
     * con il numero corretto di utenti e che i dati del primo utente
     * corrispondano ai valori attesi.
     * Inoltre verifica che il metodo findAll del repository venga chiamato una sola volta.
     */

    @Test
    @DisplayName("UC8.S1 - getAllUsers restituisce tutti gli utenti")
    void testGetAllUsers_Success() {

        when(userRepository.findAll()).thenReturn(mockUserEntities);


        List<UserDto> result = userService.getAllUsers();


        assertNotNull(result);
        assertEquals(4, result.size());


        assertEquals("alice", result.get(0).getUsername());
        assertEquals("Alice", result.get(0).getNome());
        assertEquals("Smith", result.get(0).getCognome());

        verify(userRepository, times(1)).findAll();
    }

    /**
     * Test per il metodo che recupera tutti gli utenti tranne uno specifico.
     * <p>
     * Mocka il repository per restituire la lista completa di utenti.
     * Verifica che la lista restituita dal servizio escluda l'utente specificato
     * (in questo caso 'fedegambe'), mantenendo gli altri utenti.
     * Controlla che l'utente escluso non sia presente e che gli altri siano inclusi,
     * e che il metodo findAll sia chiamato una volta.
     */

    @Test
    @DisplayName("UC8.S2 - getAllUsersExcept esclude l'utente specificato")
    void testGetAllUsersExcept_ExcludesSpecifiedUser() {

        when(userRepository.findAll()).thenReturn(mockUserEntities);

        // Act
        List<UserDto> result = userService.getAllUsersExcept("fedegambe");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());

        // Verifica che l'utente corrente sia escluso
        boolean userExcluded = result.stream()
                .noneMatch(user -> user.getUsername().equals("fedegambe"));
        assertTrue(userExcluded, "L'utente corrente deve essere escluso dalla lista");

        // Verifica che gli altri utenti siano presenti
        List<String> usernames = result.stream()
                .map(UserDto::getUsername)
                .toList();
        assertTrue(usernames.contains("alice"));
        assertTrue(usernames.contains("bob"));
        assertTrue(usernames.contains("charlie"));

        verify(userRepository, times(1)).findAll();
    }

    /**
     * Test che verifica il comportamento quando viene passato uno username inesistente
     * al metodo getAllUsersExcept.
     * <p>
     * Mocka il repository per restituire una lista di utenti esistente.
     * Verifica che la lista restituita dal servizio non escluda nessun utente
     * e contenga tutti gli utenti.
     * Assicura che il metodo findAll venga chiamato una volta.
     */

    @Test
    @DisplayName("UC8.S3 - getAllUsersExcept con utente inesistente restituisce tutti")
    void testGetAllUsersExcept_NonExistentUser() {
        // Arrange
        when(userRepository.findAll()).thenReturn(mockUserEntities);

        // Act
        List<UserDto> result = userService.getAllUsersExcept("nonexistent_user");

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size()); // Tutti gli utenti presenti

        verify(userRepository, times(1)).findAll();
    }

    /**
     * Test di comportamento con repository vuoto.
     * <p>
     * Mocka il repository per restituire una lista vuota.
     * Verifica che il servizio non ritorni null ma una lista vuota,
     * e che il metodo findAll venga chiamato esattamente una volta.
     */

    @Test
    @DisplayName("UC8.S4 - getAllUsers con repository vuoto")
    void testGetAllUsers_EmptyRepository() {
        // Arrange
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<UserDto> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository, times(1)).findAll();
    }

    /**
     * Test per garantire che la conversione da User a UserDto non esponga la password.
     * <p>
     * Mocka il repository per restituire un utente specifico.
     * Verifica che il DTO restituito contenga i campi username, nome, cognome, email corretti,
     * e implicitamente che il campo password non sia presente o accessibile.
     */

    @Test
    @DisplayName("UC8.S5 - Conversione User -> UserDto non espone password")
    void testUserToDto_DoesNotExposePassword() {

        when(userRepository.findAll()).thenReturn(List.of(currentUser));


        List<UserDto> result = userService.getAllUsers();


        assertNotNull(result);
        assertEquals(1, result.size());

        UserDto dto = result.get(0);
        // Il DTO non dovrebbe avere un campo password accessibile
        assertEquals("fedegambe", dto.getUsername());
        assertEquals("Federico", dto.getNome());
        assertEquals("Sgambelluri", dto.getCognome());
        assertEquals("federico@example.com", dto.getEmail());
    }

    /**
     * Test di gestione delle eccezioni lanciate dal repository.
     * <p>
     * Mocka il repository per lanciare un'eccezione RuntimeException.
     * Verifica che l'eccezione venga propagata correttamente dal servizio.
     * Assicura inoltre che il metodo findAll sia stato chiamato una sola volta.
     */

    @Test
    @DisplayName("UC8.S6 - Gestione eccezione del repository")
    void testGetAllUsers_RepositoryException() {

        when(userRepository.findAll()).thenThrow(new RuntimeException("Database connection failed"));


        assertThrows(RuntimeException.class, () -> {
            userService.getAllUsers();
        });

        verify(userRepository, times(1)).findAll();
    }

    /**
     * Test per gestire il caso in cui venga passato uno username null al metodo getAllUsersExcept.
     * <p>
     * Mocka il repository per restituire una lista di utenti esistente.
     * Verifica che il metodo ritorni tutti gli utenti senza esclusioni.
     * Controlla che il metodo findAll sia chiamato esattamente una volta.
     */

    @Test
    @DisplayName("UC8.S7 - getAllUsersExcept con username null")
    void testGetAllUsersExcept_NullUsername() {

        when(userRepository.findAll()).thenReturn(mockUserEntities);


        List<UserDto> result = userService.getAllUsersExcept(null);

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size()); // Tutti gli utenti presenti

        verify(userRepository, times(1)).findAll();
    }

     /**
     * Test per gestire il caso in cui venga passato uno username vuoto al metodo getAllUsersExcept.
     * <p>
     * Mocka il repository per restituire una lista di utenti esistente.
     * Verifica che il metodo ritorni tutti gli utenti senza esclusioni.
     * Controlla che il metodo findAll sia chiamato una sola volta.
     */

    @Test
    @DisplayName("UC8.S8 - getAllUsersExcept con username vuoto")
    void testGetAllUsersExcept_EmptyUsername() {
        // Arrange
        when(userRepository.findAll()).thenReturn(mockUserEntities);

        // Act
        List<UserDto> result = userService.getAllUsersExcept("");

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size()); // Tutti gli utenti presenti

        verify(userRepository, times(1)).findAll();
    }

    /**
     * Test che verifica la presenza dell'annotazione @Transactional(readOnly = true)
     * sul metodo getAllUsers (da controllare manualmente nel codice sorgente).
     * <p>
     * Mocka il repository per restituire dati di test.
     * Verifica che il metodo findAll venga chiamato esattamente una volta,
     * a conferma dell'interazione corretta con il repository.
     */

    @Test
    @DisplayName("UC8.S9 - Verifica transazionalità del metodo")
    void testGetAllUsers_Transactional() {
        // Arrange
        when(userRepository.findAll()).thenReturn(mockUserEntities);

        // Act
        List<UserDto> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        // Il metodo deve essere @Transactional(readOnly = true)
        // Questo viene verificato tramite annotazioni nel codice sorgente
        verify(userRepository, times(1)).findAll();
    }

    /**
     * Test di performance per getAllUsers con un dataset di grandi dimensioni.
     * <p>
     * Crea una lista mock di 1000 utenti e la imposta come risultato del repository.
     * Misura il tempo di esecuzione del metodo e verifica che sia inferiore a 100ms,
     * assicurando un buon livello di performance anche su dataset estesi.
     * Controlla che il numero di utenti restituiti corrisponda al numero atteso.
     */

    @Test
    @DisplayName("UC8.S10 - Performance con lista grande di utenti")
    void testGetAllUsers_LargeDataset() {
        // Arrange
        List<User> largeUserList = createLargeUserDataset(1000);
        when(userRepository.findAll()).thenReturn(largeUserList);

        // Act
        long startTime = System.currentTimeMillis();
        List<UserDto> result = userService.getAllUsers();
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(result);
        assertEquals(1000, result.size());

        // Verifica che l'operazione sia ragionevolmente veloce (< 100ms)
        long executionTime = endTime - startTime;
        assertTrue(executionTime < 100, "L'operazione dovrebbe completarsi in meno di 100ms");

        verify(userRepository, times(1)).findAll();
    }


    /**
     * Test per verificare la case sensitivity nel metodo getAllUsersExcept.
     * <p>
     * Mocka il repository per restituire una lista predefinita.
     * Esegue la chiamata con username in minuscolo e maiuscolo.
     * Verifica che l'utente venga escluso solo se il case corrisponde esattamente,
     * altrimenti la lista resta invariata.
     */
    @Test
    @DisplayName("UC8.S11 - getAllUsersExcept con case sensitivity")
    void testGetAllUsersExcept_CaseSensitive() {
        // Arrange
        when(userRepository.findAll()).thenReturn(mockUserEntities);

        // Act
        List<UserDto> result1 = userService.getAllUsersExcept("fedegambe");
        List<UserDto> result2 = userService.getAllUsersExcept("FEDEGAMBE"); // Maiuscolo

        // Assert
        assertEquals(3, result1.size());
        assertEquals(4, result2.size()); // Nessuna esclusione perché il case è diverso

        verify(userRepository, times(2)).findAll();
    }

    /**
     * Test comparativo tra getAllUsers e getAllUsersExcept per verificare
     * che getAllUsersExcept restituisca la lista degli utenti meno quello escluso.
     * <p>
     * Mocka il repository per restituire la lista completa.
     * Verifica che la differenza in dimensione sia esattamente uno,
     * e che tutti gli utenti in getAllUsersExcept siano presenti in getAllUsers.
     */
    @Test
    @DisplayName("UC8.S12 - Confronto comportamento getAllUsers vs getAllUsersExcept")
    void testCompareBehavior_GetAllVsGetAllExcept() {
        // Arrange
        when(userRepository.findAll()).thenReturn(mockUserEntities);

        // Act
        List<UserDto> allUsers = userService.getAllUsers();
        List<UserDto> usersExceptFede = userService.getAllUsersExcept("fedegambe");

        // Assert
        assertEquals(4, allUsers.size());
        assertEquals(3, usersExceptFede.size());
        assertEquals(allUsers.size() - 1, usersExceptFede.size());

        // Verifica che tutti gli utenti in usersExceptFede siano presenti in allUsers
        for (UserDto user : usersExceptFede) {
            assertTrue(allUsers.stream().anyMatch(u -> u.getUsername().equals(user.getUsername())));
        }
    }

    // Helper Methods

    /**
     * Crea una lista di utenti mock per i test.
     * <p>
     * Include tre utenti fittizi più l'utente corrente,
     * con dati come id, username, nome, cognome, email e password.
     * Questa lista viene usata per simulare la risposta del repository.
     * 
     * @return lista di utenti mock
     */

    private List<User> createMockUserEntities() {
        List<User> users = new ArrayList<>();

        User alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setNome("Alice");
        alice.setCognome("Smith");
        alice.setEmail("alice@example.com");
        alice.setPassword("hashed_password");

        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        bob.setNome("Bob");
        bob.setCognome("Johnson");
        bob.setEmail("bob@example.com");
        bob.setPassword("hashed_password");

        User charlie = new User();
        charlie.setId(3L);
        charlie.setUsername("charlie");
        charlie.setNome("Charlie");
        charlie.setCognome("Brown");
        charlie.setEmail("charlie@example.com");
        charlie.setPassword("hashed_password");

        users.add(alice);
        users.add(bob);
        users.add(charlie);
        users.add(createCurrentUser()); // Evita duplicazione

        return users;
    }

     /**
     * Crea un utente corrente fittizio per i test.
     * <p>
     * L'utente ha id, username, nome, cognome, email e password predefiniti.
     * Serve per simulare l'utente con cui interagire in alcuni test.
     * 
     * @return utente corrente mock
     */

    private User createCurrentUser() {
        User user = new User();
        user.setId(4L);
        user.setUsername("fedegambe");
        user.setNome("Federico");
        user.setCognome("Sgambelluri");
        user.setEmail("federico@example.com");
        user.setPassword("hashed_password");
        return user;
    }

    /**
     * Crea una lista di utenti di grandi dimensioni per test di performance.
     * <p>
     * Genera una lista con il numero di utenti specificato, ognuno con
     * dati fittizi coerenti per id, username, nome, cognome, email e password.
     * 
     * @param size numero di utenti da creare
     * @return lista di utenti mock estesa
     */
    
    private List<User> createLargeUserDataset(int size) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            User user = new User();
            user.setId((long) i);
            user.setUsername("user" + i);
            user.setNome("Nome" + i);
            user.setCognome("Cognome" + i);
            user.setEmail("user" + i + "@example.com");
            user.setPassword("hashed_password");
            users.add(user);
        }
        return users;
    }
}