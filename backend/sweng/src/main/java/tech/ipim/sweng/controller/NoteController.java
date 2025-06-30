package tech.ipim.sweng.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.NoteDto;
import tech.ipim.sweng.dto.NoteResponse;
import tech.ipim.sweng.service.NoteService;
import tech.ipim.sweng.util.JwtUtil;
import tech.ipim.sweng.dto.UpdateNoteRequest;
import tech.ipim.sweng.dto.PermissionDto;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin(
    origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, 
    allowedHeaders = "*",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
    allowCredentials = "true"
)
public class NoteController {

    private final NoteService noteService;
    private final JwtUtil jwtUtil;

    @Autowired
    public NoteController(NoteService noteService, JwtUtil jwtUtil) {
        this.noteService = noteService;
        this.jwtUtil = jwtUtil;
    }

  
    @PostMapping
    public ResponseEntity<?> createNote(@Valid @RequestBody CreateNoteRequest request,
                                       BindingResult bindingResult,
                                       @RequestHeader("Authorization") String authHeader) {
        
        System.out.println("POST /api/notes - Creazione nota");
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
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
            NoteDto createdNote = noteService.createNote(request, username);
            System.out.println("Nota creata con successo: " + createdNote.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(NoteResponse.success("Nota creata con successo", createdNote));

        } catch (Exception e) {
            System.err.println("Errore creazione nota: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante la creazione della nota"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllNotes(@RequestHeader("Authorization") String authHeader,
                                        @RequestParam(value = "filter", defaultValue = "all") String filter) {
        
        System.out.println("GET /api/notes - Filter: " + filter);
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            List<NoteDto> notes;
            
            switch (filter) {
                case "own":
                    notes = noteService.getUserNotes(username);
                    break;
                case "shared":
                    notes = noteService.getAllAccessibleNotes(username)
                            .stream()
                            .filter(note -> !note.getAutore().equals(username))
                            .toList();
                    break;
                default:
                    notes = noteService.getAllAccessibleNotes(username);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notes", notes,
                    "count", notes.size()
            ));

        } catch (Exception e) {
            System.err.println("Errore recupero note: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante il recupero delle note"));
        }
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<?> getNoteById(@PathVariable Long id,
                                        @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            Optional<NoteDto> note = noteService.getNoteById(id, username);
            
            if (note.isPresent()) {
                return ResponseEntity.ok(NoteResponse.success("Nota trovata", note.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(NoteResponse.error("Nota non trovata o non accessibile"));
            }

        } catch (Exception e) {
            System.err.println("Errore recupero nota: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante il recupero della nota"));
        }
    }

    
    @GetMapping("/search")
    public ResponseEntity<?> searchNotes(@RequestParam("q") String keyword,
                                        @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            List<NoteDto> notes = noteService.searchNotes(username, keyword);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notes", notes,
                    "count", notes.size(),
                    "keyword", keyword
            ));

        } catch (Exception e) {
            System.err.println("Errore ricerca note: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante la ricerca"));
        }
    }

    @GetMapping("/filter/tag/{tag}")
    public ResponseEntity<?> getNotesByTag(@PathVariable String tag,
                                          @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            List<NoteDto> notes = noteService.getNotesByTag(username, tag);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notes", notes,
                    "count", notes.size(),
                    "tag", tag
            ));

        } catch (Exception e) {
            System.err.println("Errore filtro per tag: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante il filtro per tag"));
        }
    }

    
    @GetMapping("/filter/cartella/{cartella}")
    public ResponseEntity<?> getNotesByCartella(@PathVariable String cartella,
                                               @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            List<NoteDto> notes = noteService.getNotesByCartella(username, cartella);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notes", notes,
                    "count", notes.size(),
                    "cartella", cartella
            ));

        } catch (Exception e) {
            System.err.println("Errore filtro per cartella: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante il filtro per cartella"));
        }
    }

    /**
     * Duplica una nota
     * POST /api/notes/{id}/duplicate
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<?> duplicateNote(@PathVariable Long id,
                                          @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            NoteDto duplicatedNote = noteService.duplicateNote(id, username);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(NoteResponse.success("Nota duplicata con successo", duplicatedNote));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(NoteResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Errore duplicazione nota: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante la duplicazione"));
        }
    }

    /**
     * Elimina una nota
     * DELETE /api/notes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable Long id,
                                       @RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            boolean deleted = noteService.deleteNote(id, username);
            
            if (deleted) {
                return ResponseEntity.ok(NoteResponse.success("Nota eliminata con successo"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(NoteResponse.error("Errore durante l'eliminazione"));
            }

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(NoteResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Errore eliminazione nota: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante l'eliminazione"));
        }
    }


    @DeleteMapping("/{id}/sharing")
    public ResponseEntity<?> removeFromSharing(@PathVariable Long id,
                                               @RequestHeader("Authorization") String authHeader) {

        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            noteService.removeUserFromSharing(id, username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Rimosso con successo dalla condivisione della nota"
            ));

        } catch (RuntimeException e) {
            System.err.println("Errore rimozione condivisione: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(NoteResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Errore interno rimozione condivisione: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante la rimozione dalla condivisione"));
        }
    }

    /**
     * Aggiorna una nota esistente
     * PUT /api/notes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(@PathVariable Long id,
                                        @Valid @RequestBody UpdateNoteRequest request,
                                        BindingResult bindingResult,
                                        @RequestHeader("Authorization") String authHeader) {

        System.out.println("PUT /api/notes/" + id + " - Aggiornamento nota");

        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        // Validazione dei dati
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

        // Imposta l'ID dalla URL (per sicurezza)
        request.setId(id);

        try {
            NoteDto updatedNote = noteService.updateNote(id, request, username);
            System.out.println("Nota aggiornata con successo: " + updatedNote.getId());

            return ResponseEntity.ok(NoteResponse.success("Nota aggiornata con successo", updatedNote));

        } catch (RuntimeException e) {
            System.err.println("Errore aggiornamento nota: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(NoteResponse.error(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Errore interno aggiornamento nota: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante l'aggiornamento della nota"));
        }
    }

    /**
     * Recupera statistiche utente
     * GET /api/notes/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(@RequestHeader("Authorization") String authHeader) {
        
        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            NoteService.UserStatsDto stats = noteService.getUserStats(username);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stats", Map.of(
                            "noteCreate", stats.getNoteCreate(),
                            "noteCondivise", stats.getNoteCondivise(),
                            "tagUtilizzati", stats.getTagUtilizzati(),
                            "cartelleCreate", stats.getCartelleCreate(),
                            "allTags", stats.getAllTags(),
                            "allCartelle", stats.getAllCartelle()
                    )
            ));

        } catch (Exception e) {
            System.err.println("Errore statistiche: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante il recupero delle statistiche"));
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



    @PutMapping("/{id}/permissions")
    public ResponseEntity<?> updateNotePermissions(@PathVariable Long id,
                                                   @Valid @RequestBody PermissionDto permissionDto,
                                                   BindingResult bindingResult,
                                                   @RequestHeader("Authorization") String authHeader) {

        System.out.println("PUT /api/notes/" + id + "/permissions - Modifica permessi nota");

        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
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
            NoteDto updatedNote = noteService.updateNotePermissions(id, permissionDto, username);
            System.out.println("Permessi nota aggiornati con successo: " + id);

            return ResponseEntity.ok(NoteResponse.success("Permessi aggiornati con successo", updatedNote));

        } catch (RuntimeException e) {
            System.err.println("Errore modifica permessi: " + e.getMessage());
            if (e.getMessage().contains("proprietario") || e.getMessage().contains("permessi")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(NoteResponse.error(e.getMessage()));
            } else if (e.getMessage().contains("non trovata")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(NoteResponse.error(e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(NoteResponse.error("Errore durante la modifica dei permessi"));
            }
        } catch (Exception e) {
            System.err.println("Errore interno modifica permessi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore interno del server"));
        }
    }
}