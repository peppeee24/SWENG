package tech.ipim.sweng.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
/**
 * DTO per la richiesta di aggiornamento di una nota.
 * <p>
 * Contiene i dati modificabili di una nota, con le rispettive validazioni.
 * <p>
 * Campi:
 * <ul>
 *   <li>{@code id} - identificativo univoco della nota da aggiornare</li>
 *   <li>{@code titolo} - obbligatorio, massimo 100 caratteri</li>
 *   <li>{@code contenuto} - obbligatorio, massimo 280 caratteri</li>
 *   <li>{@code tags} - set di tag associati alla nota (opzionale)</li>
 *   <li>{@code cartelle} - set di cartelle di appartenenza (opzionale)</li>
 * </ul>
 */

public class UpdateNoteRequest {

    private Long id;

    @NotBlank(message = "Il titolo è obbligatorio")
    @Size(max = 100, message = "Il titolo deve essere massimo 100 caratteri")
    private String titolo;

    @NotBlank(message = "Il contenuto è obbligatorio")
    @Size(max = 280, message = "Il contenuto deve essere massimo 280 caratteri")
    private String contenuto;

    private Set<String> tags;
    private Set<String> cartelle;

    // Constructors
    public UpdateNoteRequest() { }

    public UpdateNoteRequest(Long id, String titolo, String contenuto, Set<String> tags, Set<String> cartelle) {
        this.id = id;
        this.titolo = titolo;
        this.contenuto = contenuto;
        this.tags = tags;
        this.cartelle = cartelle;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "UpdateNoteRequest{"
                + "id=" + id
                + ", titolo='" + titolo + '\''
                + ", contenuto='" + contenuto + '\''
                + ", tags=" + tags
                + ", cartelle=" + cartelle
                + '}';
    }
}