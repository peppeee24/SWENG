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

    // Campi per il sistema di blocco
    @Column(name = "is_locked_for_editing")
    private Boolean isLockedForEditing = false;

    @Column(name = "locked_by_user")
    private String lockedByUser;

    @Column(name = "lock_expires_at")
    private LocalDateTime lockExpiresAt;

    // Campo per il versionamento
    @Column(name = "version_number")
    private Long versionNumber = 1L;

    // COSTRUTTORI
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

    // LIFECYCLE CALLBACKS
    @PrePersist
    protected void onCreate() {
        dataCreazione = LocalDateTime.now();
        dataModifica = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataModifica = LocalDateTime.now();
    }

    // GETTERS E SETTERS
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

    // Getters e Setters per il sistema di blocco
    public Boolean getIsLockedForEditing() {
        return isLockedForEditing;
    }

    public void setIsLockedForEditing(Boolean isLockedForEditing) {
        this.isLockedForEditing = isLockedForEditing;
    }

    public String getLockedByUser() {
        return lockedByUser;
    }

    public void setLockedByUser(String lockedByUser) {
        this.lockedByUser = lockedByUser;
    }

    public LocalDateTime getLockExpiresAt() {
        return lockExpiresAt;
    }

    public void setLockExpiresAt(LocalDateTime lockExpiresAt) {
        this.lockExpiresAt = lockExpiresAt;
    }

    // Getter/Setter per versionamento
    public Long getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Long versionNumber) {
        this.versionNumber = versionNumber;
    }

    // METODI DI BUSINESS LOGIC

    // Metodi per verificare autorizzazioni
    public boolean isAutore(String username) {
        return this.autore != null && this.autore.getUsername().equals(username);
    }

    public boolean hasWriteAccess(String username) {
        // L'autore ha sempre accesso in scrittura
        if (autore.getUsername().equals(username)) {
            return true;
        }

        // Solo le note condivise in scrittura permettono la scrittura ad altri
        if (tipoPermesso == TipoPermesso.CONDIVISA_SCRITTURA) {
            return permessiScrittura.contains(username);
        }

        // Tutte le altre situazioni: no accesso in scrittura
        return false;
    }

    public boolean hasReadAccess(String username) {
        // L'autore ha sempre accesso in lettura
        if (autore.getUsername().equals(username)) {
            return true;
        }

        // Nota privata: solo l'autore può leggere
        if (tipoPermesso == TipoPermesso.PRIVATA) {
            return false;
        }

        // Nota condivisa in lettura: controlla lista lettura
        if (tipoPermesso == TipoPermesso.CONDIVISA_LETTURA) {
            return permessiLettura.contains(username);
        }

        // Nota condivisa in scrittura: controlla ENTRAMBE le liste
        if (tipoPermesso == TipoPermesso.CONDIVISA_SCRITTURA) {
            return permessiLettura.contains(username) || permessiScrittura.contains(username);
        }

        return false;
    }

    // Metodi legacy per compatibilità (se necessari)
    public boolean haPermessoLettura(String username) {
        return hasReadAccess(username);
    }

    public boolean haPermessoScrittura(String username) {
        return hasWriteAccess(username);
    }

    // Metodi per il sistema di blocco
    public boolean isExpiredLock() {
        return lockExpiresAt != null && LocalDateTime.now().isAfter(lockExpiresAt);
    }

    public boolean canBeEditedBy(String username) {
        if (!isLockedForEditing || isExpiredLock()) {
            return true;
        }
        return lockedByUser != null && lockedByUser.equals(username);
    }

    public boolean isLocked() {
        return Boolean.TRUE.equals(isLockedForEditing) &&
                lockExpiresAt != null &&
                LocalDateTime.now().isBefore(lockExpiresAt);
    }

    public boolean isLockedBy(String username) {
        return isLocked() && lockedByUser != null && lockedByUser.equals(username);
    }

    public void lockFor(String username, int lockDurationMinutes) {
        this.isLockedForEditing = true;
        this.lockedByUser = username;
        this.lockExpiresAt = LocalDateTime.now().plusMinutes(lockDurationMinutes);
    }

    public void unlock() {
        this.isLockedForEditing = false;
        this.lockedByUser = null;
        this.lockExpiresAt = null;
    }

    public boolean hasExpiredLock() {
        return Boolean.TRUE.equals(isLockedForEditing) &&
                lockExpiresAt != null &&
                LocalDateTime.now().isAfter(lockExpiresAt);
    }

    // Metodi per il versionamento
    public void incrementVersion() {
        this.versionNumber = (this.versionNumber == null ? 1L : this.versionNumber + 1);
    }

    public void updateModificationTime() {
        this.dataModifica = LocalDateTime.now();
    }

    // Metodo combinato per verificare se può essere modificata
    public boolean canBeModifiedBy(String username) {
        // Prima verifica i permessi di scrittura
        if (!hasWriteAccess(username)) {
            return false;
        }

        // Poi verifica il blocco
        return canBeEditedBy(username);
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", titolo='" + titolo + '\'' +
                ", autore=" + (autore != null ? autore.getUsername() : "null") +
                ", dataCreazione=" + dataCreazione +
                ", tipoPermesso=" + tipoPermesso +
                ", isLocked=" + isLocked() +
                ", lockedBy='" + lockedByUser + '\'' +
                ", versionNumber=" + versionNumber +
                '}';
    }
}