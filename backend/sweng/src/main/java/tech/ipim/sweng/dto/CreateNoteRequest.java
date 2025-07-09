package tech.ipim.sweng.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * DTO per la richiesta di creazione di una nuova nota.
 * <p>
 * Utilizzato dal client per inviare i dati necessari alla creazione di una nota,
 * inclusi titolo, contenuto, tag, cartelle e permessi.
 * <p>
 * Campi validati:
 * <ul>
 *   <li>{@code titolo} - obbligatorio, massimo 100 caratteri</li>
 *   <li>{@code contenuto} - obbligatorio, massimo 280 caratteri</li>
 * </ul>
 * Campi opzionali:
 * <ul>
 *   <li>{@code tags} - insieme di etichette associate alla nota</li>
 *   <li>{@code cartelle} - insieme di nomi di cartelle in cui salvare la nota</li>
 *   <li>{@code permessi} - oggetto che definisce i permessi di condivisione</li>
 * </ul>
 * <p>
 * Le annotazioni {@code @NotBlank} e {@code @Size} garantiscono la validazione automatica
 * dei campi obbligatori lato backend.
 */
public class CreateNoteRequest {

    @NotBlank(message = "Titolo è obbligatorio")
    @Size(max = 100, message = "Titolo deve essere massimo 100 caratteri")
    private String titolo;

    @NotBlank(message = "Contenuto è obbligatorio")
    @Size(max = 280, message = "Contenuto deve essere massimo 280 caratteri")
    private String contenuto;

    private Set<String> tags;
    private Set<String> cartelle;
    private PermissionDto permessi;

    public CreateNoteRequest() { }

    public CreateNoteRequest(String titolo, String contenuto) {
        this.titolo = titolo;
        this.contenuto = contenuto;
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

    public PermissionDto getPermessi() {
        return permessi;
    }

    public void setPermessi(PermissionDto permessi) {
        this.permessi = permessi;
    }

    @Override
    public String toString() {
        return "CreateNoteRequest{"
                + "titolo='" + titolo + '\''
                + ", contenuto='" + contenuto + '\''
                + ", tags=" + tags
                + ", cartelle=" + cartelle
                + ", permessi=" + permessi
                + '}';
    }
}

