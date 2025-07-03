package tech.ipim.sweng.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "note_versions")
public class NoteVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String contenuto;

    @Column(name = "title", length = 200, nullable = false)
    private String titolo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 100, nullable = false)
    private String createdBy;

    @Column(columnDefinition = "TEXT")
    private String changeDescription;

    public NoteVersion() {
        this.createdAt = LocalDateTime.now();
    }

    public NoteVersion(Note note, Integer versionNumber, String contenuto, String titolo, String createdBy, String changeDescription) {
        this();
        this.note = note;
        this.versionNumber = versionNumber;
        this.contenuto = contenuto;
        this.titolo = titolo;
        this.createdBy = createdBy;
        this.changeDescription = changeDescription;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
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
}