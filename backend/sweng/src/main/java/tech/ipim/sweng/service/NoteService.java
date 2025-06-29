package tech.ipim.sweng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.NoteDto;
import tech.ipim.sweng.dto.PermissionDto;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;
import java.time.LocalDateTime;
import tech.ipim.sweng.dto.UpdateNoteRequest;
import java.util.Set;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    @Autowired
    public NoteService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    public NoteDto createNote(CreateNoteRequest request, String username) {
        User autore = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        Note note = new Note(request.getTitolo(), request.getContenuto(), autore);

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            note.setTags(request.getTags());
        }

        if (request.getCartelle() != null && !request.getCartelle().isEmpty()) {
            note.setCartelle(request.getCartelle());
        }

        configurePermissions(note, request.getPermessi());

        Note savedNote = noteRepository.save(note);
        System.out.println("Nota creata con successo: " + savedNote.getId() + " da " + username);

        return NoteDto.fromNote(savedNote, username);
    }

    private void configurePermissions(Note note, PermissionDto permissionDto) {
        if (permissionDto == null) {
            note.setTipoPermesso(Note.TipoPermesso.PRIVATA);
            note.setPermessiLettura(new HashSet<>());
            note.setPermessiScrittura(new HashSet<>());
            return;
        }

        note.setTipoPermesso(permissionDto.getTipoPermesso());

        switch (permissionDto.getTipoPermesso()) {
            case PRIVATA:
                note.setPermessiLettura(new HashSet<>());
                note.setPermessiScrittura(new HashSet<>());
                System.out.println("Nota configurata come PRIVATA");
                break;

            case CONDIVISA_LETTURA:
                note.setPermessiLettura(permissionDto.getUtentiLettura() != null ?
                        new HashSet<>(permissionDto.getUtentiLettura()) : new HashSet<>());
                note.setPermessiScrittura(new HashSet<>());
                System.out.println("Nota configurata come CONDIVISA_LETTURA con " +
                        note.getPermessiLettura().size() + " utenti");
                break;

            case CONDIVISA_SCRITTURA:
                Set<String> utentiLettura = new HashSet<>();
                if (permissionDto.getUtentiLettura() != null) {
                    utentiLettura.addAll(permissionDto.getUtentiLettura());
                }
                if (permissionDto.getUtentiScrittura() != null) {
                    utentiLettura.addAll(permissionDto.getUtentiScrittura());
                }

                note.setPermessiLettura(utentiLettura);
                note.setPermessiScrittura(permissionDto.getUtentiScrittura() != null ?
                        new HashSet<>(permissionDto.getUtentiScrittura()) : new HashSet<>());
                System.out.println("Nota configurata come CONDIVISA_SCRITTURA con " +
                        note.getPermessiLettura().size() + " utenti lettura e " +
                        note.getPermessiScrittura().size() + " utenti scrittura");
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
    public UserNotesStats getUserStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        long noteCreate = noteRepository.countByAutore(user);
        long noteCondivise = noteRepository.countSharedNotes(username);
        List<String> tags = noteRepository.findAllTagsByUser(username);
        List<String> cartelle = noteRepository.findAllCartelleByUser(username);

        return new UserNotesStats(noteCreate, noteCondivise, tags.size(), cartelle.size(), tags, cartelle);
    }

    public NoteDto duplicateNote(Long noteId, String username) {
        System.out.println("Tentativo duplicazione nota ID: " + noteId + " per utente: " + username);

        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (!noteOpt.isPresent()) {
            System.out.println("Nota con ID " + noteId + " non esiste nel database");
            throw new RuntimeException("Nota non trovata nel database");
        }

        Note originalNote = noteOpt.get();
        System.out.println("Nota trovata: " + originalNote.getTitolo() + " di " + originalNote.getAutore().getUsername());

        if (!originalNote.haPermessoLettura(username)) {
            System.out.println("Utente " + username + " non ha accesso alla nota " + noteId);
            throw new RuntimeException("Non hai i permessi per duplicare questa nota");
        }

        User autore = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        Note duplicatedNote = new Note(
                originalNote.getTitolo() + " (Copia)",
                originalNote.getContenuto(),
                autore
        );

        if (originalNote.getTags() != null && !originalNote.getTags().isEmpty()) {
            duplicatedNote.setTags(new HashSet<>(originalNote.getTags()));
        }

        if (originalNote.getCartelle() != null && !originalNote.getCartelle().isEmpty()) {
            duplicatedNote.setCartelle(new HashSet<>(originalNote.getCartelle()));
        }

        Note savedNote = noteRepository.save(duplicatedNote);
        System.out.println("Nota duplicata con successo: " + originalNote.getId() + " -> " + savedNote.getId());

        return NoteDto.fromNote(savedNote, username);
    }

    public boolean deleteNote(Long noteId, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata"));

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

        // Verifica che l'utente non sia il proprietario
        if (note.getAutore().getUsername().equals(username)) {
            throw new RuntimeException("Il proprietario non può rimuoversi dalla propria nota");
        }

        // Verifica che l'utente abbia accesso alla nota
        boolean hasAccess = note.getPermessiLettura().contains(username) ||
                note.getPermessiScrittura().contains(username);

        if (!hasAccess) {
            throw new RuntimeException("L'utente non ha accesso a questa nota");
        }

        // Rimuove l'utente dai permessi
        note.getPermessiLettura().remove(username);
        note.getPermessiScrittura().remove(username);

        // Aggiorna la data di modifica
        note.setDataModifica(LocalDateTime.now());

        // Salva le modifiche
        noteRepository.save(note);

        System.out.println("Utente " + username + " rimosso dalla condivisione della nota " + noteId);
    }

    /**
     * Aggiorna una nota esistente
     */
    @Transactional
    public NoteDto updateNote(Long noteId, UpdateNoteRequest request, String username) {
        // Trova la nota
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata"));

        // Verifica i permessi di modifica
        boolean canEdit = note.getAutore().getUsername().equals(username) ||
                note.getPermessiScrittura().contains(username);

        if (!canEdit) {
            throw new RuntimeException("Non hai i permessi per modificare questa nota");
        }

        // Aggiorna i campi
        note.setTitolo(request.getTitolo().trim());
        note.setContenuto(request.getContenuto().trim());

        // Aggiorna tags se forniti
        if (request.getTags() != null) {
            note.setTags(new HashSet<>(request.getTags()));
        } else {
            note.setTags(new HashSet<>());
        }

        // Aggiorna cartelle se fornite
        if (request.getCartelle() != null) {
            note.setCartelle(new HashSet<>(request.getCartelle()));
        } else {
            note.setCartelle(new HashSet<>());
        }

        // Aggiorna la data di modifica (dovrebbe essere automatico con @PreUpdate, ma forziamo)
        note.setDataModifica(LocalDateTime.now());

        // Salva la nota aggiornata
        Note updatedNote = noteRepository.save(note);

        System.out.println("Nota aggiornata con successo: " + updatedNote.getId() + " da " + username);

        return NoteDto.fromNote(updatedNote, username);
    }


    @Transactional
    public NoteDto updateNotePermissions(Long noteId, PermissionDto permissionDto, String username) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota non trovata"));

        if (!note.getAutore().getUsername().equals(username)) {
            throw new RuntimeException("Solo il proprietario può modificare i permessi di questa nota");
        }

        System.out.println("Aggiornamento permessi nota " + noteId + " da parte di " + username);
        System.out.println("Nuovo tipo permesso: " + permissionDto.getTipoPermesso());
        System.out.println("Utenti lettura: " + permissionDto.getUtentiLettura());
        System.out.println("Utenti scrittura: " + permissionDto.getUtentiScrittura());

        configurePermissions(note, permissionDto);

        note.setDataModifica(LocalDateTime.now());

        Note updatedNote = noteRepository.save(note);

        System.out.println("Permessi nota aggiornati con successo: " + updatedNote.getId());

        return NoteDto.fromNote(updatedNote, username);
    }

    public static class UserNotesStats {
        private final long noteCreate;
        private final long noteCondivise;
        private final long tagUtilizzati;
        private final long cartelleCreate;
        private final List<String> allTags;
        private final List<String> allCartelle;

        public UserNotesStats(long noteCreate, long noteCondivise, long tagUtilizzati,
                              long cartelleCreate, List<String> allTags, List<String> allCartelle) {
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