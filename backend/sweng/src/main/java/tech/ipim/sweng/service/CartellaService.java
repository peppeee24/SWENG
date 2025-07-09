package tech.ipim.sweng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ipim.sweng.dto.CartellaDto;
import tech.ipim.sweng.dto.CreateCartellaRequest;
import tech.ipim.sweng.dto.UpdateCartellaRequest;
import tech.ipim.sweng.model.Cartella;
import tech.ipim.sweng.model.User;
import tech.ipim.sweng.repository.CartellaRepository;
import tech.ipim.sweng.repository.UserRepository;
import tech.ipim.sweng.repository.NoteRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartellaService {

    private final CartellaRepository cartellaRepository;
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;

    @Autowired
    public CartellaService(CartellaRepository cartellaRepository, 
                          UserRepository userRepository,
                          NoteRepository noteRepository) {
        this.cartellaRepository = cartellaRepository;
        this.userRepository = userRepository;
        this.noteRepository = noteRepository;
    }

    /**
     * Crea una nuova cartella per l'utente specificato
     * 
     * @param request dati per creare la cartella (nome, descrizione, colore)
     * @param username username del proprietario della cartella
     * @return DTO della cartella creata con numero note inizializzato a 0
     * @throws RuntimeException se l'utente non esiste o esiste già una cartella con lo stesso nome
     */
    public CartellaDto createCartella(CreateCartellaRequest request, String username) {
        User proprietario = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        if (cartellaRepository.existsByNomeAndProprietario(request.getNome(), proprietario)) {
            throw new RuntimeException("Esiste già una cartella con il nome: " + request.getNome());
        }

        Cartella cartella = new Cartella(request.getNome(), proprietario);
        cartella.setDescrizione(request.getDescrizione());
        
        if (request.getColore() != null && !request.getColore().trim().isEmpty()) {
            cartella.setColore(request.getColore());
        }

        Cartella savedCartella = cartellaRepository.save(cartella);
        System.out.println("Cartella creata: " + savedCartella.getNome() + " da " + username);
        
        CartellaDto dto = CartellaDto.fromCartella(savedCartella);
        dto.setNumeroNote(0); //  cartella non ha note
        return dto;
    }

    /**
     * Recupera tutte le cartelle dell'utente ordinate per data di modifica decrescente
     * 
     * @param username username dell'utente proprietario delle cartelle
     * @return lista di DTO delle cartelle con il numero di note conteggiato
     * @throws RuntimeException se l'utente non esiste
     */
    @Transactional(readOnly = true)
    public List<CartellaDto> getUserCartelle(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));
        
        List<Cartella> cartelle = cartellaRepository.findByProprietarioOrderByDataModificaDesc(user);
        
        return cartelle.stream()
                .map(cartella -> {
                    CartellaDto dto = CartellaDto.fromCartella(cartella);
                    // Conta le note in questa cartella
                    long numeroNote = noteRepository.findNotesByCartella(username, cartella.getNome()).size();
                    dto.setNumeroNote(numeroNote);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Recupera una cartella specifica per ID e username proprietario
     * 
     * @param id ID della cartella
     * @param username username del proprietario
     * @return Optional di DTO della cartella con numero note se trovata, altrimenti empty
     */
    @Transactional(readOnly = true)
    public Optional<CartellaDto> getCartellaById(Long id, String username) {
        Optional<Cartella> cartella = cartellaRepository.findByIdAndUsername(id, username);
        
        if (cartella.isPresent()) {
            CartellaDto dto = CartellaDto.fromCartella(cartella.get());
            // Conta le note in questa cartella
            long numeroNote = noteRepository.findNotesByCartella(username, cartella.get().getNome()).size();
            dto.setNumeroNote(numeroNote);
            return Optional.of(dto);
        }
        
        return Optional.empty();
    }

    /**
     * Aggiorna i dati di una cartella esistente
     * 
     * @param id ID della cartella da aggiornare
     * @param request dati aggiornati della cartella (nome, descrizione, colore)
     * @param username username del proprietario che effettua l'aggiornamento
     * @return DTO della cartella aggiornata con il numero di note aggiornato
     * @throws RuntimeException se la cartella non esiste, non è accessibile o nome già esistente
     */
    public CartellaDto updateCartella(Long id, UpdateCartellaRequest request, String username) {
        Cartella cartella = cartellaRepository.findByIdAndUsername(id, username)
                .orElseThrow(() -> new RuntimeException("Cartella non trovata o non accessibile"));

        // Verifica se il nuovo nome è già in uso (solo se è diverso da quello attuale)
        if (!cartella.getNome().equals(request.getNome())) {
            if (cartellaRepository.existsByNomeAndProprietario(request.getNome(), cartella.getProprietario())) {
                throw new RuntimeException("Esiste già una cartella con il nome: " + request.getNome());
            }
        }

        // Aggiorna i campi
        cartella.setNome(request.getNome());
        cartella.setDescrizione(request.getDescrizione());
        
        if (request.getColore() != null && !request.getColore().trim().isEmpty()) {
            cartella.setColore(request.getColore());
        }

        Cartella savedCartella = cartellaRepository.save(cartella);
        System.out.println("Cartella aggiornata: " + savedCartella.getId() + " da " + username);
        
        CartellaDto dto = CartellaDto.fromCartella(savedCartella);
        // Conta le note in questa cartella
        long numeroNote = noteRepository.findNotesByCartella(username, savedCartella.getNome()).size();
        dto.setNumeroNote(numeroNote);
        return dto;
    }

    /**
     * Elimina una cartella se vuota di note
     * 
     * @param id ID della cartella da eliminare
     * @param username username del proprietario che richiede l'eliminazione
     * @return true se eliminazione avvenuta con successo
     * @throws RuntimeException se la cartella non esiste, non è accessibile o contiene note
     */
    public boolean deleteCartella(Long id, String username) {
        Cartella cartella = cartellaRepository.findByIdAndUsername(id, username)
                .orElseThrow(() -> new RuntimeException("Cartella non trovata o non accessibile"));

        // Verifica se ci sono note in questa cartella
        long numeroNote = noteRepository.findNotesByCartella(username, cartella.getNome()).size();
        if (numeroNote > 0) {
            throw new RuntimeException("Impossibile eliminare la cartella: contiene " + numeroNote + " note. Sposta prima le note.");
        }

        cartellaRepository.delete(cartella);
        System.out.println("Cartella eliminata: " + id + " da " + username);
        return true;
    }

    /**
     * Recupera statistiche sulle cartelle di un utente
     * 
     * @param username username dell'utente di cui ottenere le statistiche
     * @return oggetto contenente numero totale di cartelle e lista nomi cartelle
     * @throws RuntimeException se l'utente non esiste
     */
    @Transactional(readOnly = true)
    public CartelleStats getUserCartelleStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

        long numeroCartelle = cartellaRepository.countByProprietario(user);
        List<String> nomiCartelle = cartellaRepository.findByUsername(username)
                .stream()
                .map(Cartella::getNome)
                .collect(Collectors.toList());

        return new CartelleStats(numeroCartelle, nomiCartelle);
    }

    /**
     * Classe interna per rappresentare le statistiche delle cartelle di un utente
     */
    public static class CartelleStats {
        private final long numeroCartelle;
        private final List<String> nomiCartelle;

        public CartelleStats(long numeroCartelle, List<String> nomiCartelle) {
            this.numeroCartelle = numeroCartelle;
            this.nomiCartelle = nomiCartelle;
        }

        public long getNumeroCartelle() { 
            return numeroCartelle; 
        }
        
        public List<String> getNomiCartelle() {
             return nomiCartelle; 
        }
    }
}
