package tech.ipim.sweng.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RegistrationResponse {

    private boolean success;
    private String message;
    private Long userId;
    private String username;
    private String nome;
    private String cognome;
    private String email;
    private String citta;
    private LocalDate dataNascita;
    private LocalDateTime createdAt;


    public RegistrationResponse() {}

    public RegistrationResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public RegistrationResponse(boolean success, String message, Long userId, String username, LocalDateTime createdAt) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.username = username;
        this.createdAt = createdAt;
    }

    // Factory methods per creare risposte standard
    public static RegistrationResponse success(Long userId, String username, String nome, String cognome,
                                               String email, String citta, LocalDate dataNascita, LocalDateTime createdAt) {
        RegistrationResponse response = new RegistrationResponse(true, "Registrazione completata con successo", userId, username, createdAt);
        response.setNome(nome);
        response.setCognome(cognome);
        response.setEmail(email);
        response.setCitta(citta);
        response.setDataNascita(dataNascita);
        return response;
    }

    public static RegistrationResponse error(String message) {
        return new RegistrationResponse(false, message);
    }

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCitta() {
        return citta;
    }

    public void setCitta(String citta) {
        this.citta = citta;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public void setDataNascita(LocalDate dataNascita) {
        this.dataNascita = dataNascita;
    }
}