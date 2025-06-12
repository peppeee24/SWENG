package tech.ipim.sweng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ipim.sweng.dto.RegistrationRequest;
import tech.ipim.sweng.dto.RegistrationResponse;
import tech.ipim.sweng.exception.UserAlreadyExistsException;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.UserRepository;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
}