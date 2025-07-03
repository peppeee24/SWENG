package tech.ipim.sweng.dto;

import java.time.LocalDateTime;

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