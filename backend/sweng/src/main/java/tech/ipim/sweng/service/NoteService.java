package tech.ipim.sweng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.NoteDto;
import tech.ipim.sweng.dto.UpdateNoteRequest;
import tech.ipim.sweng.dto.PermissionDto;
import tech.ipim.sweng.dto.NoteVersionDto;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.NoteVersion;
import tech.ipim.sweng.dto.VersionComparisonDto;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final NoteVersionService noteVersionService;

    @Autowired

    public NoteService(NoteRepository noteRepository, UserRepository userRepository, NoteVersionService noteVersionService) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.noteVersionService = noteVersionService;
    }

    @Transactional
    public NoteDto createNote(CreateNoteRequest request, String username) {
        System.out.println("=== INIZIO CREAZIONE NOTA ===");
        System.out.println("User: " + username);
        System.out.println("Request: " + request.getTitolo());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        Note note = new Note(request.getTitolo(), request.getContenuto(), user);

        if (request.getTags() != null) {
            note.setTags(new HashSet<>(request.getTags()));
            System.out.println("Tags configurati: " + note.getTags());
        }

        if (request.getCartelle() != null) {
            note.setCartelle(new HashSet<>(request.getCartelle()));
            System.out.println("Cartelle configurate: " + note.getCartelle());
        }

        if (request.getPermessi() != null) {
            System.out.println("=== CONFIGURAZIONE PERMESSI ===");
            System.out.println("Tipo permesso ricevuto: " + request.getPermessi().getTipoPermesso());
            System.out.println("Utenti lettura ricevuti: " + request.getPermessi().getUtentiLettura());
            System.out.println("Utenti scrittura ricevuti: " + request.getPermessi().getUtentiScrittura());

            // IMPORTANTE: Assicurati che i set siano inizializzati PRIMA della configurazione
            if (note.getPermessiLettura() == null) {
                note.setPermessiLettura(new HashSet<>());
            }
            if (note.getPermessiScrittura() == null) {
                note.setPermessiScrittura(new HashSet<>());
            }

            configurePermissions(note, request.getPermessi());

            // Verifica IMMEDIATA dopo configurazione
            System.out.println("DOPO configurePermissions:");
            System.out.println("- Tipo permesso: " + note.getTipoPermesso());
            System.out.println("- Set lettura: " + note.getPermessiLettura());
            System.out.println("- Set scrittura: " + note.getPermessiScrittura());
            System.out.println("- Set lettura size: " + note.getPermessiLettura().size());
            System.out.println("- Set scrittura size: " + note.getPermessiScrittura().size());
        } else {
            System.out.println("Nessun permesso ricevuto, impostazione PRIVATA");
            note.setTipoPermesso(TipoPermesso.PRIVATA);
            note.setPermessiLettura(new HashSet<>());
            note.setPermessiScrittura(new HashSet<>());
        }

        // === SALVATAGGIO CON FLUSH ===
        System.out.println("=== PRIMA DEL SALVATAGGIO ===");
        System.out.println("Tipo permesso PRE-save: " + note.getTipoPermesso());
        System.out.println("Permessi lettura PRE-save: " + note.getPermessiLettura());
        System.out.println("Permessi scrittura PRE-save: " + note.getPermessiScrittura());

        Note savedNote = noteRepository.saveAndFlush(note); // SALVA E FORZA FLUSH

        System.out.println("=== DOPO SALVATAGGIO ===");
        System.out.println("ID salvato: " + savedNote.getId());
        System.out.println("Tipo permesso POST-save: " + savedNote.getTipoPermesso());
        System.out.println("Permessi lettura POST-save: " + savedNote.getPermessiLettura());
        System.out.println("Permessi scrittura POST-save: " + savedNote.getPermessiScrittura());

        // Crea la prima versione
        noteVersionService.createVersion(savedNote, username, "Creazione nota");

        // === VERIFICA FINALE CON RICARICAMENTO ===
        Note reloadedNote = noteRepository.findById(savedNote.getId()).orElse(null);
        if (reloadedNote != null) {
            System.out.println("=== VERIFICA RICARICAMENTO ===");
            System.out.println("Tipo permesso RICARICATO: " + reloadedNote.getTipoPermesso());
            System.out.println("Permessi lettura RICARICATI: " + reloadedNote.getPermessiLettura());
            System.out.println("Permessi scrittura RICARICATI: " + reloadedNote.getPermessiScrittura());
        }

        System.out.println("=== FINE CREAZIONE NOTA ===");
        return NoteDto.fromNote(savedNote, username);
    }

    public List<NoteDto> getAllAccessibleNotes(String username) {
        List<Note> notes = noteRepository.findAllAccessibleNotes(username);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .toList();
    }

    public List<NoteDto> getUserNotes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        List<Note> notes = noteRepository.findByAutoreOrderByDataModificaDesc(user);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .toList();
    }

    public Optional<NoteDto> getNoteById(Long noteId, String username) {
        Optional<Note> note = noteRepository.findAccessibleNoteById(noteId, username);
        return note.map(n -> NoteDto.fromNote(n, username));
    }

    public List<NoteDto> searchNotes(String username, String keyword) {
        List<Note> notes = noteRepository.searchNotesByKeyword(username, keyword.trim());
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .toList();
    }

    public List<NoteDto> getNotesByTag(String username, String tag) {
        List<Note> notes = noteRepository.findNotesByTag(username, tag);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .toList();
    }

    public List<NoteDto> getNotesByCartella(String username, String cartella) {
        List<Note> notes = noteRepository.findNotesByCartella(username, cartella);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .toList();
    }

    @Transactional
    public NoteDto duplicateNote(Long noteId, String username) {
        Note originalNote = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata: " + noteId));

        boolean hasAccess = originalNote.getAutore().getUsername().equals(username) ||
                originalNote.getPermessiLettura().contains(username) ||
                originalNote.getPermessiScrittura().contains(username);

        if (!hasAccess) {
            throw new RuntimeException("Non hai accesso a questa nota");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        Note duplicatedNote = new Note(
                originalNote.getTitolo() + " (Copia)",
                originalNote.getContenuto(),
                user
        );

        duplicatedNote.setTags(new HashSet<>(originalNote.getTags()));
        duplicatedNote.setCartelle(new HashSet<>(originalNote.getCartelle()));

        Note savedNote = noteRepository.save(duplicatedNote);

        noteVersionService.createVersion(savedNote, username, "Duplicazione da nota ID: " + noteId);

        System.out.println("Nota duplicata con successo: " + savedNote.getId() + " da " + username);

        return NoteDto.fromNote(savedNote, username);
    }

    @Transactional
    public boolean deleteNote(Long noteId, String username) {
        System.out.println("=== INIZIO ELIMINAZIONE NOTA ===");
        System.out.println("Note ID: " + noteId + ", Username: " + username);

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata: " + noteId));

        System.out.println("Nota trovata - Autore: " + note.getAutore().getUsername());
        System.out.println("Utente richiedente: " + username);
        System.out.println("È proprietario? " + note.isAutore(username));

        if (!note.isAutore(username)) {
            System.err.println(" ERRORE: L'utente " + username + " non è il proprietario della nota " + noteId);
            throw new RuntimeException("Non hai i permessi per eliminare questa nota");
        }

        try {
            // STEP 1: Elimina PRIMA tutte le versioni della nota
            System.out.println("STEP 1: Eliminazione versioni...");
            noteVersionService.deleteAllVersionsForNote(noteId);
            System.out.println("Versioni eliminate con successo");

            // STEP 2: Elimina la nota principale
            System.out.println("STEP 2: Eliminazione nota principale...");
            noteRepository.delete(note);
            System.out.println(" Nota principale eliminata con successo");

            // STEP 3: Flush per assicurarsi che tutto sia committed
            noteRepository.flush();
            System.out.println("Operazioni committed nel database");

            System.out.println("=== ELIMINAZIONE COMPLETATA CON SUCCESSO ===");
            System.out.println("Nota eliminata: " + noteId + " da " + username);

            return true;

        } catch (Exception e) {
            System.err.println(" ERRORE durante l'eliminazione della nota " + noteId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Errore durante l'eliminazione della nota: " + e.getMessage());
        }
    }

    @Transactional
    public void removeUserFromSharing(Long noteId, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata"));

        if (note.isAutore(username)) {
            throw new RuntimeException("Il proprietario non può rimuoversi dalla propria nota");
        }

        boolean hasAccess = note.getPermessiLettura().contains(username) ||
                note.getPermessiScrittura().contains(username);

        if (!hasAccess) {
            throw new RuntimeException("L'utente non ha accesso a questa nota");
        }

        note.getPermessiLettura().remove(username);
        note.getPermessiScrittura().remove(username);
        note.setDataModifica(LocalDateTime.now());

        noteRepository.save(note);
        System.out.println("Utente " + username + " rimosso dalla condivisione della nota " + noteId);
    }

    @Transactional
    public NoteDto updateNote(Long noteId, UpdateNoteRequest request, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata"));

        boolean canEdit = note.getAutore().getUsername().equals(username) ||
                note.getPermessiScrittura().contains(username);

        if (!canEdit) {
            throw new RuntimeException("Non hai i permessi per modificare questa nota");
        }

        String oldTitle = note.getTitolo();
        String oldContent = note.getContenuto();

        note.setTitolo(request.getTitolo().trim());
        note.setContenuto(request.getContenuto().trim());

        if (request.getTags() != null) {
            note.setTags(new HashSet<>(request.getTags()));
        } else {
            note.setTags(new HashSet<>());
        }

        if (request.getCartelle() != null) {
            note.setCartelle(new HashSet<>(request.getCartelle()));
        } else {
            note.setCartelle(new HashSet<>());
        }

        note.incrementVersion();
        note.setDataModifica(LocalDateTime.now());
        Note savedNote = noteRepository.save(note);

        String changeDescription = buildChangeDescription(oldTitle, oldContent, request.getTitolo().trim(), request.getContenuto().trim());
        noteVersionService.createVersion(savedNote, username, changeDescription);

        System.out.println("Nota aggiornata: " + noteId + " da " + username + " (versione " + note.getVersionNumber() + ")");
        return NoteDto.fromNote(savedNote, username);
    }

    @Transactional
    public NoteDto updateNotePermissions(Long noteId, PermissionDto permissionDto, String username) {
        System.out.println("=== INIZIO updateNotePermissions ===");
        System.out.println("Note ID: " + noteId + ", Username: " + username);
        System.out.println("Permessi ricevuti: " + permissionDto.getTipoPermesso());

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata"));

        if (!note.isAutore(username)) {
            throw new RuntimeException("Solo il proprietario può modificare i permessi di questa nota");
        }

        // Log stato PRIMA della modifica
        System.out.println("PRIMA - Tipo permesso: " + note.getTipoPermesso());
        System.out.println("PRIMA - Permessi lettura: " + note.getPermessiLettura());
        System.out.println("PRIMA - Permessi scrittura: " + note.getPermessiScrittura());

        // Configura i permessi
        configurePermissions(note, permissionDto);
        note.setDataModifica(LocalDateTime.now());

        // Log stato DOPO la configurazione
        System.out.println("DOPO CONFIG - Tipo permesso: " + note.getTipoPermesso());
        System.out.println("DOPO CONFIG - Permessi lettura: " + note.getPermessiLettura());
        System.out.println("DOPO CONFIG - Permessi scrittura: " + note.getPermessiScrittura());

        // IMPORTANTE: Usa saveAndFlush per forzare il salvataggio immediato
        Note savedNote = noteRepository.saveAndFlush(note);

        // Log stato DOPO il salvataggio
        System.out.println("DOPO SAVE - Tipo permesso: " + savedNote.getTipoPermesso());
        System.out.println("DOPO SAVE - Permessi lettura: " + savedNote.getPermessiLettura());
        System.out.println("DOPO SAVE - Permessi scrittura: " + savedNote.getPermessiScrittura());

        // Crea versione SOLO dopo il salvataggio dei permessi
        /*
        try {
            noteVersionService.createVersion(savedNote, username, "Modifica permessi");
            System.out.println("Versione creata con successo");
        } catch (Exception e) {
            System.err.println("Errore durante la creazione della versione: " + e.getMessage());
            // Non bloccare l'operazione se fallisce solo il versionamento
        }

         */

        System.out.println("Permessi aggiornati per nota: " + noteId + " da " + username);
        System.out.println("=== FINE updateNotePermissions ===");

        return NoteDto.fromNote(savedNote, username);
    }

    public List<NoteVersionDto> getNoteVersionHistory(Long noteId, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata"));

        boolean hasAccess = note.hasReadAccess(username);
        if (!hasAccess) {
            throw new RuntimeException("Non hai accesso a questa nota");
        }

        List<NoteVersion> versions = noteVersionService.getVersionHistory(noteId);
        return versions.stream()
                .map(NoteVersionDto::new)
                .toList();
    }

    public Optional<NoteVersionDto> getNoteVersion(Long noteId, Integer versionNumber, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata"));

        boolean hasAccess = note.hasReadAccess(username);
        if (!hasAccess) {
            throw new RuntimeException("Non hai accesso a questa nota");
        }

        Optional<NoteVersion> version = noteVersionService.getVersion(noteId, versionNumber);
        return version.map(NoteVersionDto::new);
    }

    /**
     * Ripristina una versione precedente di una nota
     */
    @Transactional
    public NoteDto restoreNoteVersion(Long noteId, Integer versionNumber, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata"));

        // Verifica che l'utente abbia i permessi di scrittura
        boolean canEdit = note.isAutore(username) ||
                note.getPermessiScrittura().contains(username);

        if (!canEdit) {
            throw new RuntimeException("Non hai i permessi per ripristinare versioni di questa nota");
        }

        // Recupera la versione da ripristinare
        Optional<NoteVersion> versionToRestore = noteVersionService.getVersion(noteId, versionNumber);
        if (versionToRestore.isEmpty()) {
            throw new RuntimeException("Versione " + versionNumber + " non trovata");
        }

        NoteVersion version = versionToRestore.get();

        // Salva i valori attuali per il change description
        String oldTitle = note.getTitolo();
        String oldContent = note.getContenuto();

        // Ripristina il contenuto dalla versione selezionata
        note.setTitolo(version.getTitolo());
        note.setContenuto(version.getContenuto());
        note.incrementVersion();
        note.setDataModifica(LocalDateTime.now());

        Note savedNote = noteRepository.save(note);

        // Crea una nuova versione per il ripristino
        String changeDescription = String.format("Ripristino alla versione %d", versionNumber);
        noteVersionService.createVersion(savedNote, username, changeDescription);

        System.out.println("Versione " + versionNumber + " ripristinata per nota " + noteId +
                " da " + username + " (nuova versione " + note.getVersionNumber() + ")");

        return NoteDto.fromNote(savedNote, username);
    }

    /**
     * Confronta due versioni di una nota
     */
    public VersionComparisonDto compareNoteVersions(Long noteId, Integer version1, Integer version2, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata"));

        boolean hasAccess = note.hasReadAccess(username);
        if (!hasAccess) {
            throw new RuntimeException("Non hai accesso a questa nota");
        }

        // Recupera le due versioni
        Optional<NoteVersion> v1 = noteVersionService.getVersion(noteId, version1);
        Optional<NoteVersion> v2 = noteVersionService.getVersion(noteId, version2);

        if (v1.isEmpty()) {
            throw new RuntimeException("Versione " + version1 + " non trovata");
        }
        if (v2.isEmpty()) {
            throw new RuntimeException("Versione " + version2 + " non trovata");
        }

        NoteVersionDto dto1 = new NoteVersionDto(v1.get());
        NoteVersionDto dto2 = new NoteVersionDto(v2.get());

        // Calcola le differenze
        boolean titleChanged = !v1.get().getTitolo().equals(v2.get().getTitolo());
        boolean contentChanged = !v1.get().getContenuto().equals(v2.get().getContenuto());

        String titleDiff = titleChanged ?
                String.format("'%s' → '%s'", v1.get().getTitolo(), v2.get().getTitolo()) : null;

        String contentDiff = contentChanged ?
                generateContentDiff(v1.get().getContenuto(), v2.get().getContenuto()) : null;

        VersionComparisonDto.DifferenceDto differences = new VersionComparisonDto.DifferenceDto(
                titleChanged, contentChanged, titleDiff, contentDiff
        );

        return new VersionComparisonDto(dto1, dto2, differences);
    }

    /**
     * Genera una descrizione semplificata delle differenze nel contenuto
     */
    private String generateContentDiff(String content1, String content2) {
        if (content1.equals(content2)) {
            return "Nessuna differenza";
        }

        int length1 = content1.length();
        int length2 = content2.length();

        if (length1 == length2) {
            return "Contenuto modificato (stessa lunghezza)";
        } else if (length1 > length2) {
            return String.format("Contenuto ridotto (%d → %d caratteri)", length1, length2);
        } else {
            return String.format("Contenuto ampliato (%d → %d caratteri)", length1, length2);
        }
    }

    public UserStatsDto getUserStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        long noteCreate = noteRepository.countByAutore(user);
        long noteCondivise = noteRepository.countSharedNotes(username);

        List<String> allTags = noteRepository.findAllTagsByUser(username);
        List<String> allCartelle = noteRepository.findAllCartelleByUser(username);

        return new UserStatsDto(
                noteCreate,
                noteCondivise,
                (long) allTags.size(),
                (long) allCartelle.size(),
                allTags,
                allCartelle
        );
    }

    private String buildChangeDescription(String oldTitle, String oldContent, String newTitle, String newContent) {
        StringBuilder description = new StringBuilder();

        if (!oldTitle.equals(newTitle)) {
            description.append("Titolo modificato; ");
        }

        if (!oldContent.equals(newContent)) {
            description.append("Contenuto modificato; ");
        }

        if (description.length() == 0) {
            description.append("Modifica minore");
        } else {
            description.setLength(description.length() - 2);
        }

        return description.toString();
    }

    private void configurePermissions(Note note, PermissionDto permissionDto) {
        System.out.println(">>> configurePermissions START");
        System.out.println("Tipo ricevuto: " + permissionDto.getTipoPermesso());
        System.out.println("Utenti lettura ricevuti: " + permissionDto.getUtentiLettura());
        System.out.println("Utenti scrittura ricevuti: " + permissionDto.getUtentiScrittura());

        // CRITICO: Assicurati che i set esistano e siano inizializzati
        if (note.getPermessiLettura() == null) {
            note.setPermessiLettura(new HashSet<>());
            System.out.println("Inizializzato permessiLettura (era null)");
        }
        if (note.getPermessiScrittura() == null) {
            note.setPermessiScrittura(new HashSet<>());
            System.out.println("Inizializzato permessiScrittura (era null)");
        }

        // Pulisci sempre i set prima di configurare
        note.getPermessiLettura().clear();
        note.getPermessiScrittura().clear();
        System.out.println("Set puliti");

        // Imposta il tipo di permesso
        note.setTipoPermesso(permissionDto.getTipoPermesso());
        System.out.println("Tipo impostato: " + note.getTipoPermesso());

        // Configura in base al tipo
        switch (permissionDto.getTipoPermesso()) {
            case PRIVATA:
                System.out.println(">>> Configurazione PRIVATA");
                // I set sono già vuoti
                break;

            case CONDIVISA_LETTURA:
                System.out.println(">>> Configurazione CONDIVISA_LETTURA");
                if (permissionDto.getUtentiLettura() != null && !permissionDto.getUtentiLettura().isEmpty()) {
                    note.getPermessiLettura().addAll(permissionDto.getUtentiLettura());
                    System.out.println("Aggiunti utenti lettura: " + note.getPermessiLettura());
                } else {
                    System.out.println("Nessun utente lettura specificato");
                }
                break;

            case CONDIVISA_SCRITTURA:
                System.out.println(">>> Configurazione CONDIVISA_SCRITTURA");

                // Aggiungi utenti con permessi di lettura
                if (permissionDto.getUtentiLettura() != null && !permissionDto.getUtentiLettura().isEmpty()) {
                    note.getPermessiLettura().addAll(permissionDto.getUtentiLettura());
                    System.out.println("Aggiunti utenti lettura: " + note.getPermessiLettura());
                }

                // Aggiungi utenti con permessi di scrittura
                if (permissionDto.getUtentiScrittura() != null && !permissionDto.getUtentiScrittura().isEmpty()) {
                    note.getPermessiScrittura().addAll(permissionDto.getUtentiScrittura());
                    System.out.println("Aggiunti utenti scrittura: " + note.getPermessiScrittura());
                }
                break;

            default:
                System.out.println("Tipo permesso non riconosciuto: " + permissionDto.getTipoPermesso());
                throw new RuntimeException("Tipo di permesso non valido: " + permissionDto.getTipoPermesso());
        }

        // Log finale
        System.out.println("FINALE - Tipo: " + note.getTipoPermesso());
        System.out.println("FINALE - Lettura (" + note.getPermessiLettura().size() + "): " + note.getPermessiLettura());
        System.out.println("FINALE - Scrittura (" + note.getPermessiScrittura().size() + "): " + note.getPermessiScrittura());
        System.out.println(">>> configurePermissions END");
    }

    public static class UserStatsDto {
        private final long noteCreate;
        private final long noteCondivise;
        private final long tagUtilizzati;
        private final long cartelleCreate;
        private final List<String> allTags;
        private final List<String> allCartelle;

        public UserStatsDto(long noteCreate, long noteCondivise, long tagUtilizzati, long cartelleCreate, List<String> allTags, List<String> allCartelle) {
            this.noteCreate = noteCreate;
            this.noteCondivise = noteCondivise;
            this.tagUtilizzati = tagUtilizzati;
            this.cartelleCreate = cartelleCreate;
            this.allTags = allTags;
            this.allCartelle = allCartelle;
        }

        public long getNoteCreate() { return noteCreate; }
        public long getNoteCondivise() { return noteCondivise; }
        public long getTagUtilizzati() { return tagUtilizzati; }
        public long getCartelleCreate() { return cartelleCreate; }
        public List<String> getAllTags() { return allTags; }
        public List<String> getAllCartelle() { return allCartelle; }
    }
}