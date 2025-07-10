package tech.ipim.sweng.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import tech.ipim.sweng.dto.LoginRequest;
import tech.ipim.sweng.dto.LoginResponse;
import tech.ipim.sweng.dto.RegistrationRequest;
import tech.ipim.sweng.dto.RegistrationResponse;
import tech.ipim.sweng.exception.UserAlreadyExistsException;
import tech.ipim.sweng.service.UserService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
    origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, 
    allowedHeaders = "*",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
    allowCredentials = "true"
)

public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }


    /**
     * checkEmailAvailability
     * Endpoint che Verifica se un'email è disponibile per la registrazione.
     * GET /api/auth/check-email?email=test@example.com
     *
     * @param email l'email da controllare
     * @return ResponseEntity con informazioni sulla disponibilità dell'email
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmailAvailability(@RequestParam String email) {

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "available", false,
                    "message", "Email non può essere vuota"
            ));
        }

        boolean available = userService.isEmailAvailable(email);

        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available ? "Email disponibile" : "Email già in uso"
        ));
    }



    /**
     * registerUser
     * Endpoint che Registra un nuovo utente dopo aver validato i dati forniti.
     * POST /api/auth/register
     *
     * @param request        oggetto con i dati di registrazione
     * @param bindingResult  risultato della validazione del form
     * @return ResponseEntity con l'esito della registrazione o messaggi di errore
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationRequest request,
                                          BindingResult bindingResult) {

        // Gestione errori di validazione
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Errori di validazione",
                    "errors", errors
            ));
        }

        try {
            RegistrationResponse response = userService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    RegistrationResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    RegistrationResponse.error("Errore interno del server")
            );
        }
    }

    /**
     * Endpoint per il login di un utente
     * POST /api/auth/login
     * 
     * @param request Dati di login (username e password)
     * @param bindingResult Risultati della validazione
     * @return ResponseEntity con JWT token e dati utente o errori
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest request, 
                                      BindingResult bindingResult) {
        
        System.out.println("POST /api/auth/login ricevuto");
        System.out.println("Tentativo login per: " + request.getUsername());
        
        // Gestione errori di validazione dei campi
        if (bindingResult.hasErrors()) {
            System.out.println("Errori di validazione login: " + bindingResult.getAllErrors());
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> 
                errors.put(error.getField(), error.getDefaultMessage())
            );
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Errori di validazione");
            errorResponse.put("errors", errors);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            // Tenta l'autenticazione
            LoginResponse response = userService.authenticateUser(request);
            System.out.println("Login completato per: " + request.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            // Credenziali non valide
            System.out.println("Login fallito per: " + request.getUsername() + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                LoginResponse.error("Username o password non corretti")
            );
            
        } catch (Exception e) {
            // Errore generico del server
            System.err.println("Errore durante il login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                LoginResponse.error("Errore interno del server durante il login")
            );
        }
    }

    /**
     * Endpoint per verificare la disponibilità di un username
     * GET /api/auth/check-username?username=test
     */
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsernameAvailability(@RequestParam String username) {

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "available", false,
                    "message", "Username non può essere vuoto"
            ));
        }

        boolean available = userService.isUsernameAvailable(username);

        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available ? "Username disponibile" : "Username già in uso"
        ));
    }

    /**
     * Endpoint di test per verificare che il server sia attivo
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "SWENG Backend is running",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}