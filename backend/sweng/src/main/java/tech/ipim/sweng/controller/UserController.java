package tech.ipim.sweng.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import tech.ipim.sweng.dto.UserDto;
import tech.ipim.sweng.service.UserService;
import tech.ipim.sweng.util.JwtUtil;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(
        origins = {"http://localhost:4200", "http://127.0.0.1:4200"},
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.OPTIONS},
        allowCredentials = "true"
)
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }


    /**
     * getAllUsers
     * Restituisce la lista di tutti gli utenti tranne quello attualmente autenticato.
     *
     * @param authHeader header Authorization contenente il JWT
     * @return ResponseEntity con la lista degli utenti o errore HTTP
     */
    @GetMapping("/list")
    public ResponseEntity<List<UserDto>> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        System.out.println("CONTROLLER CHIAMATO! UserController.getAllUsers()");
        System.out.println("Timestamp: " + java.time.LocalDateTime.now());

        try {
            // Estrai l'username dell'utente autenticato dal token JWT
            String currentUsername = extractUsernameFromAuth(authHeader);

            if (currentUsername == null) {
                System.out.println("Token non valido o mancante");
                return ResponseEntity.status(401).build(); // Unauthorized
            }

            System.out.println("Utente autenticato: " + currentUsername);

            // Ottieni tutti gli utenti ECCETTO l'utente attualmente loggato
            List<UserDto> users = userService.getAllUsersExcept(currentUsername);
            System.out.println("Service ha ritornato " + users.size() + " utenti (escluso utente corrente)");

            return ResponseEntity.ok(users);

        } catch (Exception e) {
            System.out.println("Errore nel service: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private String extractUsernameFromAuth(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.extractUsername(token);
            } catch (Exception e) {
                System.out.println("Errore estrazione username: " + e.getMessage());
                return null;
            }
        }
        return null;
    }
}