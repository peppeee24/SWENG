package tech.ipim.sweng.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import tech.ipim.sweng.dto.*;
import tech.ipim.sweng.service.NoteLockService;
import tech.ipim.sweng.service.NoteService;
import tech.ipim.sweng.util.JwtUtil;

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
    private final NoteLockService noteLockService;

    @Autowired
    public NoteController(NoteService noteService, JwtUtil jwtUtil, NoteLockService noteLockService) {
        this.noteService = noteService;
        this.jwtUtil = jwtUtil;
        this.noteLockService = noteLockService;
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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(@PathVariable Long id,
                                        @Valid @RequestBody UpdateNoteRequest request,
                                        BindingResult bindingResult,
                                        @RequestHeader("Authorization") String authHeader) {

        System.out.println("PUT /api/notes/" + id + " - Aggiornamento nota con verifica blocco");

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
            LockStatusDto lockStatus = noteLockService.getLockStatus(id, username);

            if (lockStatus.isLocked() && !lockStatus.canEdit()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(NoteResponse.error("La nota è in modifica da " + lockStatus.getLockedBy()));
            }

            if (!lockStatus.isLocked() && !noteLockService.tryLockNote(id, username)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(NoteResponse.error("Impossibile acquisire il lock sulla nota"));
            }

            request.setId(id);
            NoteDto updatedNote = noteService.updateNote(id, request, username);
            noteLockService.unlockNote(id, username);

            System.out.println("Nota aggiornata e sbloccata con successo: " + updatedNote.getId());
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

    @GetMapping("/{id}/compare/{version1}/{version2}")
    public ResponseEntity<?> compareNoteVersions(@PathVariable Long id,
                                                 @PathVariable Integer version1,
                                                 @PathVariable Integer version2,
                                                 @RequestHeader("Authorization") String authHeader) {

        System.out.println("GET /api/notes/" + id + "/compare/" + version1 + "/" + version2 + " - Confronto versioni");

        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            VersionComparisonDto comparison = noteService.compareNoteVersions(id, version1, version2, username);
            System.out.println("Confronto versioni completato con successo");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Confronto completato con successo",
                    "data", comparison
            ));

        } catch (RuntimeException e) {
            System.err.println("Errore confronto versioni: " + e.getMessage());
            if (e.getMessage().contains("accesso")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(NoteResponse.error(e.getMessage()));
            } else if (e.getMessage().contains("non trovata")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(NoteResponse.error(e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(NoteResponse.error("Errore durante il confronto delle versioni"));
            }
        } catch (Exception e) {
            System.err.println("Errore interno confronto versioni: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore interno del server"));
        }
    }


    @PostMapping("/{id}/lock")
    public ResponseEntity<?> lockNote(@PathVariable Long id,
                                      @RequestHeader("Authorization") String authHeader) {

        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            boolean locked = noteLockService.tryLockNote(id, username);

            if (locked) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Nota bloccata per la modifica",
                        "lockedBy", username
                ));
            } else {
                String lockedBy = noteLockService.getNoteLockOwner(id);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(NoteResponse.error("Nota già in modifica da " + lockedBy));
            }

        } catch (Exception e) {
            System.err.println("Errore blocco nota: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante il blocco della nota"));
        }
    }

    @DeleteMapping("/{id}/lock")
    public ResponseEntity<?> unlockNote(@PathVariable Long id,
                                        @RequestHeader("Authorization") String authHeader) {

        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            noteLockService.unlockNote(id, username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Nota sbloccata con successo"
            ));

        } catch (Exception e) {
            System.err.println("Errore sblocco nota: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante lo sblocco della nota"));
        }
    }

    @GetMapping("/{id}/lock-status")
    public ResponseEntity<?> getLockStatus(@PathVariable Long id,
                                           @RequestHeader("Authorization") String authHeader) {

        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            LockStatusDto status = noteLockService.getLockStatus(id, username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "locked", status.isLocked(),
                    "lockedBy", status.getLockedBy(),
                    "lockExpiresAt", status.getLockExpiresAt(),
                    "canEdit", status.canEdit()
            ));

        } catch (Exception e) {
            System.err.println("Errore stato blocco: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante il recupero dello stato del blocco"));
        }
    }

    @PutMapping("/{id}/extend-lock")
    public ResponseEntity<?> extendLock(@PathVariable Long id,
                                        @RequestHeader("Authorization") String authHeader) {

        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            boolean canEdit = noteLockService.canUserEditNote(id, username);

            if (!canEdit) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(NoteResponse.error("Non puoi estendere il blocco di questa nota"));
            }

            noteLockService.extendNoteLock(id, username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Blocco esteso con successo"
            ));

        } catch (Exception e) {
            System.err.println("Errore estensione blocco: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore durante l'estensione del blocco"));
        }
    }



    /**
     * Recupera la cronologia delle versioni di una nota
     */
    @GetMapping("/{id}/versions")
    public ResponseEntity<?> getNoteVersionHistory(@PathVariable Long id,
                                                   @RequestHeader("Authorization") String authHeader) {

        System.out.println("GET /api/notes/" + id + "/versions - Recupero cronologia versioni");

        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            List<NoteVersionDto> versions = noteService.getNoteVersionHistory(id, username);
            System.out.println("Cronologia versioni recuperata: " + versions.size() + " versioni");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cronologia recuperata con successo",
                    "data", versions
            ));

        } catch (RuntimeException e) {
            System.err.println("Errore recupero cronologia: " + e.getMessage());
            if (e.getMessage().contains("accesso")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(NoteResponse.error(e.getMessage()));
            } else if (e.getMessage().contains("non trovata")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(NoteResponse.error(e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(NoteResponse.error("Errore durante il recupero della cronologia"));
            }
        } catch (Exception e) {
            System.err.println("Errore interno recupero cronologia: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore interno del server"));
        }
    }

    /**
     * Recupera una versione specifica di una nota
     */
    @GetMapping("/{id}/versions/{versionNumber}")
    public ResponseEntity<?> getNoteVersion(@PathVariable Long id,
                                            @PathVariable Integer versionNumber,
                                            @RequestHeader("Authorization") String authHeader) {

        System.out.println("GET /api/notes/" + id + "/versions/" + versionNumber + " - Recupero versione specifica");

        String username = extractUsernameFromAuth(authHeader);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(NoteResponse.error("Token non valido"));
        }

        try {
            Optional<NoteVersionDto> version = noteService.getNoteVersion(id, versionNumber, username);

            if (version.isPresent()) {
                System.out.println("Versione " + versionNumber + " recuperata con successo");


                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Versione recuperata con successo",
                        "data", version.get()
                ));

            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(NoteResponse.error("Versione non trovata"));
            }

        } catch (RuntimeException e) {
            System.err.println("Errore recupero versione: " + e.getMessage());
            if (e.getMessage().contains("accesso")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(NoteResponse.error(e.getMessage()));
            } else if (e.getMessage().contains("non trovata")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(NoteResponse.error(e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(NoteResponse.error("Errore durante il recupero della versione"));
            }
        } catch (Exception e) {
            System.err.println("Errore interno recupero versione: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore interno del server"));
        }
    }

    /**
     * Ripristina una versione precedente di una nota
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restoreNoteVersion(@PathVariable Long id,
                                                @Valid @RequestBody RestoreVersionRequest request,
                                                BindingResult bindingResult,
                                                @RequestHeader("Authorization") String authHeader) {

        System.out.println("POST /api/notes/" + id + "/restore - Ripristino versione " + request.getVersionNumber());

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
            NoteDto restoredNote = noteService.restoreNoteVersion(id, request.getVersionNumber(), username);
            System.out.println("Versione " + request.getVersionNumber() + " ripristinata con successo");

            return ResponseEntity.ok(NoteResponse.success("Versione ripristinata con successo", restoredNote));

        } catch (RuntimeException e) {
            System.err.println("Errore ripristino versione: " + e.getMessage());
            if (e.getMessage().contains("permessi") || e.getMessage().contains("proprietario")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(NoteResponse.error(e.getMessage()));
            } else if (e.getMessage().contains("non trovata")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(NoteResponse.error(e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(NoteResponse.error(e.getMessage()));
            }
        } catch (Exception e) {
            System.err.println("Errore interno confronto versioni: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(NoteResponse.error("Errore interno del server"));
        }
    }

    // METODI HELPER


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



