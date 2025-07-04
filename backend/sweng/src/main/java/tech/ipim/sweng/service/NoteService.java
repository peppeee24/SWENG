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
    // private final NoteVersionService noteVersionService;

    @Autowired
    public NoteService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        // this.noteVersionService = noteVersionService;
    }

    @Transactional
    public NoteDto createNote(CreateNoteRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        Note note = new Note(request.getTitolo(), request.getContenuto(), user);

        if (request.getTags() != null) {
            note.setTags(new HashSet<>(request.getTags()));
        }

        if (request.getCartelle() != null) {
            note.setCartelle(new HashSet<>(request.getCartelle()));
        }

        if (request.getPermessi() != null) {
            configurePermissions(note, request.getPermessi());
        }

        Note savedNote = noteRepository.save(note);

        // noteVersionService.createVersion(savedNote, username, "Creazione iniziale");

        System.out.println("Nota creata con successo: " + savedNote.getId() + " da " + username);

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
                // (Prima era copia, ho messo c minuscola altrimenti crea problemi comn github action)
                originalNote.getTitolo() + " (copia)",
                originalNote.getContenuto(),
                user
        );

        duplicatedNote.setTags(new HashSet<>(originalNote.getTags()));
        duplicatedNote.setCartelle(new HashSet<>(originalNote.getCartelle()));

        Note savedNote = noteRepository.save(duplicatedNote);

        // noteVersionService.createVersion(savedNote, username, "Duplicazione da nota ID: " + noteId);

        System.out.println("Nota duplicata con successo: " + savedNote.getId() + " da " + username);

        return NoteDto.fromNote(savedNote, username);
    }

    @Transactional
    public boolean deleteNote(Long noteId, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata: " + noteId));

        if (!note.isAutore(username)) {
            throw new RuntimeException("Non hai i permessi per eliminare questa nota");
        }

        noteRepository.delete(note);
        System.out.println("Nota eliminata: " + noteId + " da " + username);
        return true;
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

        // String changeDescription = buildChangeDescription(oldTitle, oldContent, request.getTitolo().trim(), request.getContenuto().trim());
        // noteVersionService.createVersion(savedNote, username, changeDescription);

        System.out.println("Nota aggiornata: " + noteId + " da " + username + " (versione " + note.getVersionNumber() + ")");
        return NoteDto.fromNote(savedNote, username);
    }

    @Transactional
    public NoteDto updateNotePermissions(Long noteId, PermissionDto permissionDto, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata"));

        if (!note.isAutore(username)) {
            throw new RuntimeException("Solo il proprietario può modificare i permessi di questa nota");
        }

        configurePermissions(note, permissionDto);
        note.setDataModifica(LocalDateTime.now());

        Note savedNote = noteRepository.save(note);

        // noteVersionService.createVersion(savedNote, username, "Modifica permessi");

        System.out.println("Permessi aggiornati per nota: " + noteId + " da " + username);

        return NoteDto.fromNote(savedNote, username);
    }

    /*
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
    */

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
        note.setTipoPermesso(permissionDto.getTipoPermesso());

        switch (permissionDto.getTipoPermesso()) {
            case PRIVATA:
                note.getPermessiLettura().clear();
                note.getPermessiScrittura().clear();
                break;

            case CONDIVISA_LETTURA:
                Set<String> letturaSet = new HashSet<>();
                if (permissionDto.getUtentiLettura() != null) {
                    letturaSet.addAll(permissionDto.getUtentiLettura());
                }
                note.setPermessiLettura(letturaSet);
                note.getPermessiScrittura().clear();
                break;

            case CONDIVISA_SCRITTURA:
                Set<String> letturaSetScrittura = new HashSet<>();
                Set<String> scritturaSet = new HashSet<>();

                if (permissionDto.getUtentiLettura() != null) {
                    letturaSetScrittura.addAll(permissionDto.getUtentiLettura());
                }
                if (permissionDto.getUtentiScrittura() != null) {
                    scritturaSet.addAll(permissionDto.getUtentiScrittura());
                    letturaSetScrittura.addAll(permissionDto.getUtentiScrittura());
                }

                note.setPermessiLettura(letturaSetScrittura);
                note.setPermessiScrittura(scritturaSet);
                break;
        }
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