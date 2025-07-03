package tech.ipim.sweng.dto;

public class NoteLockStatusDto {

    private boolean locked;
    private boolean canEdit;
    private String lockedBy;
    private String message;

    public NoteLockStatusDto() {
    }

    public NoteLockStatusDto(boolean locked, boolean canEdit, String lockedBy, String message) {
        this.locked = locked;
        this.canEdit = canEdit;
        this.lockedBy = lockedBy;
        this.message = message;
    }

    public static NoteLockStatusDto unlocked() {
        return new NoteLockStatusDto(false, true, null, "Nota disponibile per la modifica");
    }

    public static NoteLockStatusDto lockedByUser(String username) {
        return new NoteLockStatusDto(true, true, username, "Nota bloccata da te per la modifica");
    }

    public static NoteLockStatusDto lockedByOther(String username) {
        return new NoteLockStatusDto(true, false, username, "Nota bloccata da " + username);
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}