package tech.ipim.sweng.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import tech.ipim.sweng.dto.CartellaDto;
import tech.ipim.sweng.dto.CartellaResponse;
import tech.ipim.sweng.dto.CreateCartellaRequest;
import tech.ipim.sweng.dto.UpdateCartellaRequest;
import tech.ipim.sweng.service.CartellaService;
import tech.ipim.sweng.util.JwtUtil;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cartelle")
@CrossOrigin(
    origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, 
    allowedHeaders = "*",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
    allowCredentials = "true"
)
public class CartellaController {

    private final CartellaService cartellaService;
    private final JwtUtil jwtUtil;

    @Autowired
    public CartellaController(CartellaService cartellaService, JwtUtil jwtUtil) {
        this.cartellaService = cartellaService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Crea una nuova cartella
     * POST /api/cartelle
     */
    @PostMapping
    public ResponseEntity<?> createCartella(@Valid @RequestBody CreateCartellaRequest request,
                                           BindingResult bindingResult,
                                           @RequestHeader("Authorization") String authHeader) {
        
        System.out.println("POST /api/cartelle - Creazione cartella: " + request.getNome());
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CartellaResponse.error("Token non valido"));
        }

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
            CartellaDto createdCartella = cartellaService.createCartella(request, username);
            System.out.println("Cartella creata con successo: " + createdCartella.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(CartellaResponse.success("Cartella creata con successo", createdCartella));

        } catch (RuntimeException e) {
            System.err.println("Errore creazione cartella: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CartellaResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Errore generico creazione cartella: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CartellaResponse.error("Errore durante la creazione della cartella"));
        }
    }

    /**
     * Recupera tutte le cartelle dell'utente
     * GET /api/cartelle
     */
    @GetMapping
    public ResponseEntity<?> getAllCartelle(@RequestHeader("Authorization") String authHeader) {
        
        System.out.println("GET /api/cartelle - Recupero cartelle utente");
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CartellaResponse.error("Token non valido"));
        }

        try {
            List<CartellaDto> cartelle = cartellaService.getUserCartelle(username);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "cartelle", cartelle,
                    "count", cartelle.size()
            ));

        } catch (Exception e) {
            System.err.println("Errore recupero cartelle: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CartellaResponse.error("Errore durante il recupero delle cartelle"));
        }
    }

    /**
     * Recupera una cartella specifica
     * GET /api/cartelle/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCartellaById(@PathVariable Long id,
                                            @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CartellaResponse.error("Token non valido"));
        }

        try {
            Optional<CartellaDto> cartella = cartellaService.getCartellaById(id, username);
            
            if (cartella.isPresent()) {
                return ResponseEntity.ok(CartellaResponse.success("Cartella trovata", cartella.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CartellaResponse.error("Cartella non trovata"));
            }

        } catch (Exception e) {
            System.err.println("Errore recupero cartella: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CartellaResponse.error("Errore durante il recupero della cartella"));
        }
    }

    /**
     * Aggiorna una cartella
     * PUT /api/cartelle/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCartella(@PathVariable Long id,
                                           @Valid @RequestBody UpdateCartellaRequest request,
                                           BindingResult bindingResult,
                                           @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CartellaResponse.error("Token non valido"));
        }

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
            CartellaDto updatedCartella = cartellaService.updateCartella(id, request, username);
            return ResponseEntity.ok(CartellaResponse.success("Cartella aggiornata con successo", updatedCartella));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CartellaResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Errore aggiornamento cartella: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CartellaResponse.error("Errore durante l'aggiornamento"));
        }
    }

    /**
     * Elimina una cartella
     * DELETE /api/cartelle/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCartella(@PathVariable Long id,
                                           @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CartellaResponse.error("Token non valido"));
        }

        try {
            boolean deleted = cartellaService.deleteCartella(id, username);
            
            if (deleted) {
                return ResponseEntity.ok(CartellaResponse.success("Cartella eliminata con successo"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(CartellaResponse.error("Errore durante l'eliminazione"));
            }

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CartellaResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Errore eliminazione cartella: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CartellaResponse.error("Errore durante l'eliminazione"));
        }
    }

    /**
     * Recupera statistiche cartelle utente
     * GET /api/cartelle/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getCartelleStats(@RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CartellaResponse.error("Token non valido"));
        }

        try {
            CartellaService.CartelleStats stats = cartellaService.getUserCartelleStats(username);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stats", Map.of(
                            "numeroCartelle", stats.getNumeroCartelle(),
                            "nomiCartelle", stats.getNomiCartelle()
                    )
            ));

        } catch (Exception e) {
            System.err.println("Errore statistiche cartelle: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CartellaResponse.error("Errore durante il recupero delle statistiche"));
        }
    }

    /**
     * Estrae lo username dal token JWT
     */
    private String extractUsernameFromAuth(String authHeader) {
        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token != null && jwtUtil.isTokenValid(token)) {
                return jwtUtil.extractUsername(token);
            }
        } catch (Exception e) {
            System.err.println("Errore validazione token: " + e.getMessage());
        }
        return null;
    }
}