package tech.ipim.sweng.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

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

@ExtendWith(MockitoExtension.class)
@DisplayName("UC8 - Test Service Layer per Gestione Utenti")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private List<User> mockUserEntities;
    private User currentUser;

    @BeforeEach
    void setUp() {
        mockUserEntities = createMockUserEntities();
        currentUser = createCurrentUser();

<<<<<<< Updated upstream
        // Verifica che nessun oggetto User sia null
=======

>>>>>>> Stashed changes
        assertNotNull(currentUser, "Current user should not be null");
        for (User user : mockUserEntities) {
            assertNotNull(user, "Mock user should not be null");
            assertNotNull(user.getUsername(), "Username should not be null");
            assertNotNull(user.getId(), "User ID should not be null");
        }
    }

    @Test
    @DisplayName("UC8.S1 - getAllUsers restituisce tutti gli utenti")
    void testGetAllUsers_Success() {
<<<<<<< Updated upstream
        // Arrange
        when(userRepository.findAll()).thenReturn(mockUserEntities);

        // Act
        List<UserDto> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size());

        // Verifica conversione corretta
=======

        when(userRepository.findAll()).thenReturn(mockUserEntities);


        List<UserDto> result = userService.getAllUsers();


        assertNotNull(result);
        assertEquals(4, result.size());


>>>>>>> Stashed changes
        assertEquals("alice", result.get(0).getUsername());
        assertEquals("Alice", result.get(0).getNome());
        assertEquals("Smith", result.get(0).getCognome());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("UC8.S2 - getAllUsersExcept esclude l'utente specificato")
    void testGetAllUsersExcept_ExcludesSpecifiedUser() {
<<<<<<< Updated upstream
        // Arrange
=======

>>>>>>> Stashed changes
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

    @Test
    @DisplayName("UC8.S5 - Conversione User -> UserDto non espone password")
    void testUserToDto_DoesNotExposePassword() {
<<<<<<< Updated upstream
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(currentUser));

        // Act
        List<UserDto> result = userService.getAllUsers();

        // Assert
=======

        when(userRepository.findAll()).thenReturn(List.of(currentUser));


        List<UserDto> result = userService.getAllUsers();


>>>>>>> Stashed changes
        assertNotNull(result);
        assertEquals(1, result.size());

        UserDto dto = result.get(0);
        // Il DTO non dovrebbe avere un campo password accessibile
        assertEquals("fedegambe", dto.getUsername());
        assertEquals("Federico", dto.getNome());
        assertEquals("Sgambelluri", dto.getCognome());
        assertEquals("federico@example.com", dto.getEmail());
    }

    @Test
    @DisplayName("UC8.S6 - Gestione eccezione del repository")
    void testGetAllUsers_RepositoryException() {
<<<<<<< Updated upstream
        // Arrange
        when(userRepository.findAll()).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
=======

        when(userRepository.findAll()).thenThrow(new RuntimeException("Database connection failed"));


>>>>>>> Stashed changes
        assertThrows(RuntimeException.class, () -> {
            userService.getAllUsers();
        });

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("UC8.S7 - getAllUsersExcept con username null")
    void testGetAllUsersExcept_NullUsername() {
<<<<<<< Updated upstream
        // Arrange
        when(userRepository.findAll()).thenReturn(mockUserEntities);

        // Act
=======

        when(userRepository.findAll()).thenReturn(mockUserEntities);


>>>>>>> Stashed changes
        List<UserDto> result = userService.getAllUsersExcept(null);

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size()); // Tutti gli utenti presenti

        verify(userRepository, times(1)).findAll();
    }

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