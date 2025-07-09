package tech.ipim.sweng.dto;

import java.time.LocalDateTime;

/**
 * DTO che rappresenta lo stato di blocco (lock) di una nota.
 * <p>
 * Utilizzato per informare il client se una nota è attualmente bloccata da un altro utente,
 * chi ha effettuato il blocco, quando scadrà, e se l’utente corrente può modificarla.
 * <p>
 * Campi:
 * <ul>
 *   <li>{@code isLocked} - true se la nota è attualmente bloccata</li>
 *   <li>{@code lockedBy} - username dell’utente che ha effettuato il lock</li>
 *   <li>{@code lockExpiresAt} - data e ora di scadenza del blocco</li>
 *   <li>{@code canEdit} - true se l’utente corrente ha diritto a modificare la nota</li>
 * </ul>
 */
public class LockStatusDto {
    private boolean isLocked;
    private String lockedBy;
    private LocalDateTime lockExpiresAt;
    private boolean canEdit;

    public LockStatusDto() {
    }

    public LockStatusDto(boolean isLocked, String lockedBy, LocalDateTime lockExpiresAt, boolean canEdit) {
        this.isLocked = isLocked;
        this.lockedBy = lockedBy;
        this.lockExpiresAt = lockExpiresAt;
        this.canEdit = canEdit;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public LocalDateTime getLockExpiresAt() {
        return lockExpiresAt;
    }

    public void setLockExpiresAt(LocalDateTime lockExpiresAt) {
        this.lockExpiresAt = lockExpiresAt;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }
}