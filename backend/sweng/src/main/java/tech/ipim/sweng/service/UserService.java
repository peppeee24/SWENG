package tech.ipim.sweng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.ipim.sweng.dto.LoginRequest;
import tech.ipim.sweng.dto.LoginResponse;
import tech.ipim.sweng.dto.RegistrationRequest;
import tech.ipim.sweng.dto.RegistrationResponse;
import tech.ipim.sweng.dto.UserDto;
import tech.ipim.sweng.exception.UserAlreadyExistsException;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.util.JwtUtil;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registra un nuovo utente nel sistema
     * @param request i dati di registrazione
     * @return la risposta della registrazione
     * @throws UserAlreadyExistsException se l'username o email esistono già
     */
    public RegistrationResponse registerUser(RegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' è già in uso");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email '" + request.getEmail() + "' è già in uso");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setNome(request.getNome());
        user.setCognome(request.getCognome());
        user.setEmail(request.getEmail());
        user.setSesso(request.getSesso());
        user.setNumeroTelefono(request.getNumeroTelefono());
        user.setCitta(request.getCitta());
        user.setDataNascita(request.getDataNascita());

        User savedUser = userRepository.save(user);

        return RegistrationResponse.success(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getNome(),
                savedUser.getCognome(),
                savedUser.getEmail(),
                savedUser.getCitta(),
                savedUser.getDataNascita(),
                savedUser.getCreatedAt()
        );
    }

    /**
     * Verifica se un username è disponibile
     * @param username l'username da verificare
     * @return true se disponibile, false se già in uso
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Verifica se un'email è disponibile
     * @param email l'email da verificare
     * @return true se disponibile, false se già in uso
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Trova un utente per username
     * @param username l'username da cercare
     * @return l'utente se trovato
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Trova un utente per email
     * @param email l'email da cercare
     * @return l'utente se trovato
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Valida un JWT token e restituisce l'utente associato
     * @param token il JWT token da validare
     * @return l'utente se il token è valido
     */
    @Transactional(readOnly = true)
    public User validateTokenAndGetUser(String token) {
        try {
            if (!jwtUtil.isTokenValid(token)) {
                return null;
            }
            
            String username = jwtUtil.extractUsername(token);
            User user = findByUsername(username);
            
            if (user != null && jwtUtil.validateToken(token, user)) {
                return user;
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Errore validazione token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Autentica un utente (login)
     * @param request i dati di login
     * @return la risposta del login con token JWT
     * @throws RuntimeException se le credenziali non sono valide
     */
    public LoginResponse authenticateUser(LoginRequest request) {
        System.out.println("Tentativo login per username: " + request.getUsername());
        
        // Cerca l'utente per username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Credenziali non valide"));
        
        // Verifica la password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            System.out.println("Password non corretta per utente: " + request.getUsername());
            throw new RuntimeException("Credenziali non valide");
        }
        
        System.out.println("Login riuscito per utente: " + user.getUsername());
        
        // Genera JWT token
        String token = jwtUtil.generateToken(user);
        
        // Crea UserDto (senza password)
        UserDto userDto = UserDto.fromUser(user);
        
        // Crea e restituisce la risposta di successo
        return LoginResponse.success(token, userDto);
    }

    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
    }
    public List<UserDto> getAllUsersExcept(String excludeUsername) {
        List<User> users = userRepository.findAll();
        return users.stream()
                .filter(user -> !user.getUsername().equals(excludeUsername))
                .map(UserDto::fromUser)
                .collect(Collectors.toList());
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNome(user.getNome());
        dto.setCognome(user.getCognome());
        return dto;
    }
}

