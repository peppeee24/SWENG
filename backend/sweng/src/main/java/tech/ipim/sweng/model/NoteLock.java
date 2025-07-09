package tech.ipim.sweng.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
/**
 * Entità JPA che rappresenta un blocco (lock) applicato a una nota.
 * 
 * Serve a gestire la modifica concorrente di una nota, impedendo a più utenti
 * di modificarla contemporaneamente.
 * 
 * La tabella ha un vincolo di unicità su noteId per assicurare che una nota possa
 * avere al massimo un blocco attivo alla volta.
 * 
 * Campi:
 * - noteId: ID della nota bloccata (univoco)
 * - lockedBy: username dell'utente che ha acquisito il lock
 * - lockedAt: timestamp di inizio blocco
 * - expiresAt: timestamp di scadenza del blocco (lock temporaneo)
 */

@Entity
@Table(name = "note_locks", uniqueConstraints = @UniqueConstraint(columnNames = "noteId"))
public class NoteLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long noteId;

    @Column(nullable = false, length = 100)
    private String lockedBy;

    @Column(nullable = false)
    private LocalDateTime lockedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public NoteLock() {
    }

    public NoteLock(Long noteId, String lockedBy, LocalDateTime lockedAt, LocalDateTime expiresAt) {
        this.noteId = noteId;
        this.lockedBy = lockedBy;
        this.lockedAt = lockedAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}