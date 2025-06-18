package tech.ipim.sweng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ipim.sweng.dto.CreateNoteRequest;
import tech.ipim.sweng.dto.NoteDto;
import tech.ipim.sweng.model.Note;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.NoteRepository;
import tech.ipim.sweng.repository.UserRepository;

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

    /**
     * Crea una nuova nota
     */
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

        Note savedNote = noteRepository.save(note);
        System.out.println("Nota creata con successo: " + savedNote.getId() + " da " + username);
        
        return NoteDto.fromNote(savedNote, username);
    }

    /**
     * Recupera tutte le note accessibili all'utente
     */
    @Transactional(readOnly = true)
    public List<NoteDto> getAllAccessibleNotes(String username) {
        List<Note> notes = noteRepository.findAllAccessibleNotes(username);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    /**
     * Recupera solo le note create dall'utente
     */
    @Transactional(readOnly = true)
    public List<NoteDto> getUserNotes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));
        
        List<Note> notes = noteRepository.findByAutoreOrderByDataModificaDesc(user);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    /**
     * Recupera una nota specifica se l'utente ha accesso
     */
    @Transactional(readOnly = true)
    public Optional<NoteDto> getNoteById(Long noteId, String username) {
        Optional<Note> note = noteRepository.findAccessibleNoteById(noteId, username);
        return note.map(n -> NoteDto.fromNote(n, username));
    }

    /**
     * Cerca note per parole chiave
     */
    @Transactional(readOnly = true)
    public List<NoteDto> searchNotes(String username, String keyword) {
        List<Note> notes = noteRepository.searchNotesByKeyword(username, keyword);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    /**
     * Filtra note per tag
     */
    @Transactional(readOnly = true)
    public List<NoteDto> getNotesByTag(String username, String tag) {
        List<Note> notes = noteRepository.findNotesByTag(username, tag);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    /**
     * Filtra note per cartella
     */
    @Transactional(readOnly = true)
    public List<NoteDto> getNotesByCartella(String username, String cartella) {
        List<Note> notes = noteRepository.findNotesByCartella(username, cartella);
        return notes.stream()
                .map(note -> NoteDto.fromNote(note, username))
                .collect(Collectors.toList());
    }

    /**
     * Recupera statistiche utente
     */
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

    /**
     * Duplica una nota
     */
    public NoteDto duplicateNote(Long noteId, String username) {
        System.out.println("Tentativo duplicazione nota ID: " + noteId + " per utente: " + username);
        
        // Prima prova a cercare la nota normalmente
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (!noteOpt.isPresent()) {
            System.out.println("Nota con ID " + noteId + " non esiste nel database");
            throw new RuntimeException("Nota non trovata nel database");
        }
        
        Note originalNote = noteOpt.get();
        System.out.println("Nota trovata: " + originalNote.getTitolo() + " di " + originalNote.getAutore().getUsername());
        
        // Verifica se l'utente ha accesso alla nota
        if (!originalNote.haPermessoLettura(username)) {
            System.out.println("Utente " + username + " non ha accesso alla nota " + noteId);
            throw new RuntimeException("Non hai i permessi per duplicare questa nota");
        }

        User autore = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        // Crea la nota duplicata
        Note duplicatedNote = new Note(
                originalNote.getTitolo() + " (Copia)",
                originalNote.getContenuto(),
                autore
        );
        
        // Copia tags e cartelle se presenti
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

    /**
     * Elimina una nota (solo se l'utente è l'autore)
     */
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

    /**
     * Classe per le statistiche utente
     */
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

        // Getters
        public long getNoteCreate() { return noteCreate; }
        public long getNoteCondivise() { return noteCondivise; }
        public long getTagUtilizzati() { return tagUtilizzati; }
        public long getCartelleCreate() { return cartelleCreate; }
        public List<String> getAllTags() { return allTags; }
        public List<String> getAllCartelle() { return allCartelle; }
    }
}