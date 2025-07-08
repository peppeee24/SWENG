package tech.ipim.sweng.dto;

import java.time.LocalDateTime;

public class VersionComparisonDto {
    private Long noteId;
    private Integer version1Number;
    private Integer version2Number;
    private String version1Title;
    private String version2Title;
    private String version1Content;
    private String version2Content;
    private String version1CreatedBy;
    private String version2CreatedBy;
    private LocalDateTime version1CreatedAt;
    private LocalDateTime version2CreatedAt;
    private boolean titleChanged;
    private boolean contentChanged;
    private String changesSummary;
    private DifferenceDto differences;

    public VersionComparisonDto() { }

    public VersionComparisonDto(Long noteId, Integer v1Number, Integer v2Number) {
        this.noteId = noteId;
        this.version1Number = v1Number;
        this.version2Number = v2Number;
    }

    public VersionComparisonDto(NoteVersionDto dto1, NoteVersionDto dto2, DifferenceDto differences) {
        this.noteId = dto1.getNoteId();
        this.version1Number = dto1.getVersionNumber();
        this.version2Number = dto2.getVersionNumber();
        this.version1Title = dto1.getTitolo();
        this.version2Title = dto2.getTitolo();
        this.version1Content = dto1.getContenuto();
        this.version2Content = dto2.getContenuto();
        this.version1CreatedBy = dto1.getCreatedBy();
        this.version2CreatedBy = dto2.getCreatedBy();
        this.version1CreatedAt = dto1.getCreatedAt();
        this.version2CreatedAt = dto2.getCreatedAt();
        this.differences = differences;
        this.titleChanged = differences.titleChanged;
        this.contentChanged = differences.contentChanged;
        updateChangesSummary();
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public Integer getVersion1Number() { 
        return version1Number; 
    }

    public void setVersion1Number(Integer version1Number) { 
        this.version1Number = version1Number; 
    }

    public Integer getVersion2Number() { 
        return version2Number; 
    }

    public void setVersion2Number(Integer version2Number) { 
        this.version2Number = version2Number; 
    }

    public String getVersion1Title() { 
        return version1Title; 
    }

    public void setVersion1Title(String version1Title) {
        this.version1Title = version1Title;
    }

    public String getVersion2Title() { 
        return version2Title; 
    }

    public void setVersion2Title(String version2Title) {
        this.version2Title = version2Title;
        updateTitleChanged();
    }

    public String getVersion1Content() { 
        return version1Content; 
    }
    
    public void setVersion1Content(String version1Content) {
        this.version1Content = version1Content;
    }

    public String getVersion2Content() { 
        return version2Content; 
    }

    public void setVersion2Content(String version2Content) {
        this.version2Content = version2Content;
        updateContentChanged();
    }

    public String getVersion1CreatedBy() { 
        return version1CreatedBy; 
    }

    public void setVersion1CreatedBy(String version1CreatedBy) { 
        this.version1CreatedBy = version1CreatedBy; 
    }

    public String getVersion2CreatedBy() { 
        return version2CreatedBy; 
    }

    public void setVersion2CreatedBy(String version2CreatedBy) { 
        this.version2CreatedBy = version2CreatedBy; 
    }

    public LocalDateTime getVersion1CreatedAt() { 
        return version1CreatedAt; 
    }
    
    public void setVersion1CreatedAt(LocalDateTime version1CreatedAt) { 
        this.version1CreatedAt = version1CreatedAt; 
    }

    public LocalDateTime getVersion2CreatedAt() { 
        return version2CreatedAt; 
    }

    public void setVersion2CreatedAt(LocalDateTime version2CreatedAt) { 
        this.version2CreatedAt = version2CreatedAt; 
    }

    public boolean isTitleChanged() { 
        return titleChanged; 
    
    }

    public void setTitleChanged(boolean titleChanged) { 
        this.titleChanged = titleChanged; 
    }

    public boolean isContentChanged() { 
        return contentChanged; 
    }
    
    public void setContentChanged(boolean contentChanged) { 
        this.contentChanged = contentChanged; 
    }

    public String getChangesSummary() { 
        return changesSummary; 
    
    }

    public void setChangesSummary(String changesSummary) { 
        this.changesSummary = changesSummary; 
    }

    public DifferenceDto getDifferences() { 
        return differences; 
    }

    public void setDifferences(DifferenceDto differences) { 
        this.differences = differences; 
    }

    private void updateTitleChanged() {
        if (version1Title != null && version2Title != null) {
            this.titleChanged = !version1Title.equals(version2Title);
            updateChangesSummary();
        }
    }

    private void updateContentChanged() {
        if (version1Content != null && version2Content != null) {
            this.contentChanged = !version1Content.equals(version2Content);
            updateChangesSummary();
        }
    }

    private void updateChangesSummary() {
        StringBuilder summary = new StringBuilder();
        if (titleChanged) {
            summary.append("Titolo modificato; ");
        }
        if (contentChanged) {
            summary.append("Contenuto modificato; ");
        }
        if (summary.length() == 0) {
            summary.append("Nessuna modifica rilevata");
        } else {
            summary.setLength(summary.length() - 2); // Rimuovi ultimo "; "
        }
        this.changesSummary = summary.toString();
    }

    // Classe interna per le differenze
    public static class DifferenceDto {
        private boolean titleChanged;
        private boolean contentChanged;
        private String titleDiff;
        private String contentDiff;

        public DifferenceDto() { }

        public DifferenceDto(boolean titleChanged, boolean contentChanged, String titleDiff, String contentDiff) {
            this.titleChanged = titleChanged;
            this.contentChanged = contentChanged;
            this.titleDiff = titleDiff;
            this.contentDiff = contentDiff;
        }

        // Getters e Setters
        public boolean isTitleChanged() { 
            return titleChanged; 
        }

        public void setTitleChanged(boolean titleChanged) {
             this.titleChanged = titleChanged; 
        }

        public boolean isContentChanged() { 
            return contentChanged; 
        }
        
        public void setContentChanged(boolean contentChanged) { 
            this.contentChanged = contentChanged; 
        }

        public String getTitleDiff() { 
            return titleDiff; 
        }
        
        public void setTitleDiff(String titleDiff) { 
            this.titleDiff = titleDiff; 
        }

        public String getContentDiff() { 
            return contentDiff; 
        }
        
        public void setContentDiff(String contentDiff) {
             this.contentDiff = contentDiff; 
        }
    }
}