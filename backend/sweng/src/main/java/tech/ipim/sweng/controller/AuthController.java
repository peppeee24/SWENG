package tech.ipim.sweng.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import tech.ipim.sweng.dto.RegistrationRequest;
import tech.ipim.sweng.dto.RegistrationResponse;
import tech.ipim.sweng.exception.UserAlreadyExistsException;
import tech.ipim.sweng.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200") // Per collegarlo ad Angular
public class AuthController
{

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Endpoint per la registrazione di un nuovo utente
     * POST /api/auth/register
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