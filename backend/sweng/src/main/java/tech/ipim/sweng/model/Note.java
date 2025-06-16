package tech.ipim.sweng.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Titolo è obbligatorio")
    @Size(max = 100, message = "Titolo deve essere massimo 100 caratteri")
    private String titolo;

    @Column(nullable = false, length = 280)
    @NotBlank(message = "Contenuto è obbligatorio")
    @Size(max = 280, message = "Contenuto deve essere massimo 280 caratteri")
    private String contenuto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autore_id", nullable = false)
    private User autore;

    @Column(name = "data_creazione")
    private LocalDateTime dataCreazione;

    @Column(name = "data_modifica")
    private LocalDateTime dataModifica;

    @ElementCollection
    @CollectionTable(name = "note_tags", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "note_cartelle", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "cartella")
    private Set<String> cartelle = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_permesso")
    private TipoPermesso tipoPermesso = TipoPermesso.PRIVATA;

    @ElementCollection
    @CollectionTable(name = "note_permessi_lettura", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "username")
    private Set<String> permessiLettura = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "note_permessi_scrittura", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "username")
    private Set<String> permessiScrittura = new HashSet<>();

    public enum TipoPermesso {
        PRIVATA, CONDIVISA_LETTURA, CONDIVISA_SCRITTURA
    }

    public Note() {
        this.dataCreazione = LocalDateTime.now();
        this.dataModifica = LocalDateTime.now();
    }

    public Note(String titolo, String contenuto, User autore) {
        this();
        this.titolo = titolo;
        this.contenuto = contenuto;
        this.autore = autore;
    }

    @PreUpdate
    public void preUpdate() {
        this.dataModifica = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getContenuto() {
        return contenuto;
    }

    public void setContenuto(String contenuto) {
        this.contenuto = contenuto;
    }

    public User getAutore() {
        return autore;
    }

    public void setAutore(User autore) {
        this.autore = autore;
    }

    public LocalDateTime getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(LocalDateTime dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public LocalDateTime getDataModifica() {
        return dataModifica;
    }

    public void setDataModifica(LocalDateTime dataModifica) {
        this.dataModifica = dataModifica;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Set<String> getCartelle() {
        return cartelle;
    }

    public void setCartelle(Set<String> cartelle) {
        this.cartelle = cartelle;
    }

    public TipoPermesso getTipoPermesso() {
        return tipoPermesso;
    }

    public void setTipoPermesso(TipoPermesso tipoPermesso) {
        this.tipoPermesso = tipoPermesso;
    }

    public Set<String> getPermessiLettura() {
        return permessiLettura;
    }

    public void setPermessiLettura(Set<String> permessiLettura) {
        this.permessiLettura = permessiLettura;
    }

    public Set<String> getPermessiScrittura() {
        return permessiScrittura;
    }

    public void setPermessiScrittura(Set<String> permessiScrittura) {
        this.permessiScrittura = permessiScrittura;
    }

    public boolean isAutore(String username) {
        return this.autore != null && this.autore.getUsername().equals(username);
    }

    public boolean haPermessoLettura(String username) {
        return isAutore(username) || 
               this.tipoPermesso != TipoPermesso.PRIVATA && 
               (this.permessiLettura.contains(username) || this.permessiScrittura.contains(username));
    }

    public boolean haPermessoScrittura(String username) {
        return isAutore(username) || 
               (this.tipoPermesso == TipoPermesso.CONDIVISA_SCRITTURA && 
                this.permessiScrittura.contains(username));
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", titolo='" + titolo + '\'' +
                ", autore=" + (autore != null ? autore.getUsername() : "null") +
                ", dataCreazione=" + dataCreazione +
                ", tipoPermesso=" + tipoPermesso +
                '}';
    }
}