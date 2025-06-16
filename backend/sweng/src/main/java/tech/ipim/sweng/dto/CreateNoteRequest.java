package tech.ipim.sweng.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public class CreateNoteRequest {
    
    @NotBlank(message = "Titolo è obbligatorio")
    @Size(max = 100, message = "Titolo deve essere massimo 100 caratteri")
    private String titolo;
    
    @NotBlank(message = "Contenuto è obbligatorio")
    @Size(max = 280, message = "Contenuto deve essere massimo 280 caratteri")
    private String contenuto;
    
    private Set<String> tags;
    private Set<String> cartelle;
    
    public CreateNoteRequest() {}
    
    public CreateNoteRequest(String titolo, String contenuto) {
        this.titolo = titolo;
        this.contenuto = contenuto;
    }

    // Getters e Setters
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
        return "CreateNoteRequest{" +
                "titolo='" + titolo + '\'' +
                ", contenuto='" + contenuto + '\'' +
                ", tags=" + tags +
                ", cartelle=" + cartelle +
                '}';
    }
}

// Classe UpdateNoteRequest separata
class UpdateNoteRequest {
    
    @NotBlank(message = "Titolo è obbligatorio")
    @Size(max = 100, message = "Titolo deve essere massimo 100 caratteri")
    private String titolo;
    
    @NotBlank(message = "Contenuto è obbligatorio")
    @Size(max = 280, message = "Contenuto deve essere massimo 280 caratteri")
    private String contenuto;
    
    private Set<String> tags;
    private Set<String> cartelle;
    
    public UpdateNoteRequest() {}

    // Getters e Setters
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
        return "UpdateNoteRequest{" +
                "titolo='" + titolo + '\'' +
                ", contenuto='" + contenuto + '\'' +
                ", tags=" + tags +
                ", cartelle=" + cartelle +
                '}';
    }
}