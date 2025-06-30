package tech.ipim.sweng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.NoteDto;
import tech.ipim.sweng.dto.PermissionDto;
import tech.ipim.sweng.dto.UpdateNoteRequest;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.TipoPermesso;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class NoteService {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    public NoteDto createNote(CreateNoteRequest request, String username) {
        User autore = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        Note note = new Note();
        note.setTitolo(request.getTitolo());
        note.setContenuto(request.getContenuto());
        note.setAutore(autore);

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            note.setTags(new HashSet<>(request.getTags()));
        }

        if (request.getCartelle() != null && !request.getCartelle().isEmpty()) {
            note.setCartelle(new HashSet<>(request.getCartelle()));
        }

        configurePermissions(note, request.getPermessi());

        Note savedNote = noteRepository.save(note);
        System.out.println("Nota creata con successo: " + savedNote.getId() + " da " + username);

        return NoteDto.fromNote(savedNote, username);
    }

    private void configurePermissions(Note note, PermissionDto permissionDto) {
        if (permissionDto == null) {
            note.setTipoPermesso(TipoPermesso.PRIVATA);
            note.setPermessiLettura(new HashSet<>());
            note.setPermessiScrittura(new HashSet<>());
            return;
        }

        note.setTipoPermesso(permissionDto.getTipoPermesso());

        switch (permissionDto.getTipoPermesso()) {
            case PRIVATA:
                note.setPermessiLettura(new HashSet<>());
                note.setPermessiScrittura(new HashSet<>());
                break;
            case CONDIVISA_LETTURA:
                note.setPermessiLettura(permissionDto.getUtentiLettura() != null ?
                        new HashSet<>(permissionDto.getUtentiLettura()) : new HashSet<>());
                note.setPermessiScrittura(new HashSet<>());
                break;
            case CONDIVISA_SCRITTURA:
                note.setPermessiLettura(permissionDto.getUtentiLettura() != null ?
                        new HashSet<>(permissionDto.getUtentiLettura()) : new HashSet<>());
                note.setPermessiScrittura(permissionDto.getUtentiScrittura() != null ?
                        new HashSet<>(permissionDto.getUtentiScrittura()) : new HashSet<>());
                break;
        }
    }

    @Transactional(readOnly = true)
    public List<NoteDto> getAllAccessibleNotes(String username) {
        List<Note> notes = noteRepository.findAllAccessibleNotes(username);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NoteDto> getUserNotes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        List<Note> notes = noteRepository.findByAutoreOrderByDataModificaDesc(user);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<NoteDto> getNoteById(Long noteId, String username) {
        Optional<Note> note = noteRepository.findAccessibleNoteById(noteId, username);
        return note.map(n -> NoteDto.fromNote(n, username));
    }

    @Transactional(readOnly = true)
    public List<NoteDto> searchNotes(String username, String keyword) {
        List<Note> notes = noteRepository.searchNotesByKeyword(username, keyword);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NoteDto> getNotesByTag(String username, String tag) {
        List<Note> notes = noteRepository.findNotesByTag(username, tag);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NoteDto> getNotesByCartella(String username, String cartella) {
        List<Note> notes = noteRepository.findNotesByCartella(username, cartella);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NoteDto> getNotesByAutore(String username, String autore) {
        List<Note> notes = noteRepository.findNotesByAutore(username, autore);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NoteDto> getNotesByDataCreazione(String username, LocalDateTime dataCreazione) {
        List<Note> notes = noteRepository.findNotesByDataCreazione(username, dataCreazione);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NoteDto> getNotesByDataModifica(String username, LocalDateTime dataModifica) {
        List<Note> notes = noteRepository.findNotesByDataModifica(username, dataModifica);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserStatsDto getUserStats(String username) {
        long noteCreate = noteRepository.countNotesByAutore(username);
        long noteCondivise = noteRepository.countSharedNotesForUser(username);
        List<String> allTags = noteRepository.findAllTagsByUser(username);
        List<String> allCartelle = noteRepository.findAllCartelleByUser(username);

        return new UserStatsDto(noteCreate, noteCondivise, allTags.size(), allCartelle.size(), allTags, allCartelle);
    }

    @Transactional
    public NoteDto duplicateNote(Long noteId, String username) {
        Note originalNote = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata: " + noteId));

        if (!originalNote.haPermessoLettura(username)) {
            throw new RuntimeException("Non hai i permessi per duplicare questa nota");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        Note duplicatedNote = new Note();
        duplicatedNote.setTitolo(originalNote.getTitolo() + " (copia)");
        duplicatedNote.setContenuto(originalNote.getContenuto());
        duplicatedNote.setAutore(user);
        duplicatedNote.setTags(new HashSet<>(originalNote.getTags()));
        duplicatedNote.setCartelle(new HashSet<>(originalNote.getCartelle()));

        Note savedNote = noteRepository.save(duplicatedNote);
        System.out.println("Nota duplicata con successo: " + savedNote.getId() + " da " + username);

        return NoteDto.fromNote(savedNote, username);
    }

    @Transactional
    public boolean deleteNote(Long noteId, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata: " + noteId));

        if (!note.isAutore(username)) {
            throw new RuntimeException("Solo il proprietario può eliminare la nota");
        }

        noteRepository.delete(note);
        System.out.println("Nota eliminata: " + noteId + " da " + username);
        return true;
    }

    @Transactional
    public void removeUserFromSharing(Long noteId, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata: " + noteId));

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

        note.setDataModifica(LocalDateTime.now());
        Note savedNote = noteRepository.save(note);

        System.out.println("Nota aggiornata: " + noteId + " da " + username);
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
        System.out.println("Permessi aggiornati per nota: " + noteId + " da " + username);

        return NoteDto.fromNote(savedNote, username);
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