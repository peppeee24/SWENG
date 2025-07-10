package tech.ipim.sweng.dto;
/**
 * DTO per la risposta relativa alle operazioni sulle note.
 * <p>
 * Contiene un indicatore di successo, un messaggio descrittivo
 * e opzionalmente il DTO della nota coinvolta nell’operazione.
 * <p>
 * Fornisce metodi factory statici per costruire facilmente risposte
 * di successo o di errore.
 */

public class NoteResponse {
    
    private boolean success;
    private String message;
    private NoteDto note;
    
    public NoteResponse() { }
    
    public NoteResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public NoteResponse(boolean success, String message, NoteDto note) {
        this.success = success;
        this.message = message;
        this.note = note;
    }
    
    public static NoteResponse success(String message, NoteDto note) {
        return new NoteResponse(true, message, note);
    }
    
    public static NoteResponse success(String message) {
        return new NoteResponse(true, message);
    }
    
    public static NoteResponse error(String message) {
        return new NoteResponse(false, message);
    }

    // Getters e Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NoteDto getNote() {
        return note;
    }

    public void setNote(NoteDto note) {
        this.note = note;
    }
}