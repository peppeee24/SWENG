package tech.ipim.sweng.dto;

import tech.ipim.sweng.model.Note;
import java.time.LocalDateTime;
import java.util.Set;

public class NoteDto {
    
    private Long id;
    private String titolo;
    private String contenuto;
    private String autore;
    private LocalDateTime dataCreazione;
    private LocalDateTime dataModifica;
    private Set<String> tags;
    private Set<String> cartelle;
    private String tipoPermesso;
    private Set<String> permessiLettura;
    private Set<String> permessiScrittura;
    private boolean canEdit;
    private boolean canDelete;
    
    public NoteDto() {}
    
    public NoteDto(Note note, String currentUsername) {
        this.id = note.getId();
        this.titolo = note.getTitolo();
        this.contenuto = note.getContenuto();
        this.autore = note.getAutore().getUsername();
        this.dataCreazione = note.getDataCreazione();
        this.dataModifica = note.getDataModifica();
        this.tags = note.getTags();
        this.cartelle = note.getCartelle();
        this.tipoPermesso = note.getTipoPermesso().name();
        this.permessiLettura = note.getPermessiLettura();
        this.permessiScrittura = note.getPermessiScrittura();
        this.canEdit = note.haPermessoScrittura(currentUsername);
        this.canDelete = note.isAutore(currentUsername);
    }
    
    public static NoteDto fromNote(Note note, String currentUsername) {
        return new NoteDto(note, currentUsername);
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

    public String getAutore() {
        return autore;
    }

    public void setAutore(String autore) {
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

    public String getTipoPermesso() {
        return tipoPermesso;
    }

    public void setTipoPermesso(String tipoPermesso) {
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

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }
}