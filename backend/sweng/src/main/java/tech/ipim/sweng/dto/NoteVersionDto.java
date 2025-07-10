package tech.ipim.sweng.dto;

import tech.ipim.sweng.model.NoteVersion;
import java.time.LocalDateTime;
/**
 * DTO per la rappresentazione di una versione di una nota.
 * <p>
 * Contiene i dati relativi a una specifica versione, come il contenuto,
 * il titolo, l’autore della modifica, la data di creazione della versione
 * e una descrizione del cambiamento effettuato.
 * <p>
 * Viene costruito a partire dall’entità NoteVersion.
 */

public class NoteVersionDto {

    private Long id;
    private Long noteId;
    private Integer versionNumber;
    private String contenuto;
    private String titolo;
    private LocalDateTime createdAt;
    private String createdBy;
    private String changeDescription;

    public NoteVersionDto() {
    }

    public NoteVersionDto(NoteVersion version) {
        this.id = version.getId();
        this.noteId = version.getNote().getId();
        this.versionNumber = version.getVersionNumber();
        this.contenuto = version.getContenuto();
        this.titolo = version.getTitolo();
        this.createdAt = version.getCreatedAt();
        this.createdBy = version.getCreatedBy();
        this.changeDescription = version.getChangeDescription();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getContenuto() {
        return contenuto;
    }

    public void setContenuto(String contenuto) {
        this.contenuto = contenuto;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

}