package tech.ipim.sweng.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import tech.ipim.sweng.model.User;

/**
 * DTO per rappresentare i dati di un utente nelle risposte.
 * <p>
 * Include le informazioni essenziali dell'utente, escluse quelle sensibili come la password.
 * <p>
 * Campi:
 * <ul>
 *   <li>{@code id} - identificatore univoco dell'utente</li>
 *   <li>{@code username} - nome utente</li>
 *   <li>{@code nome} - nome proprio</li>
 *   <li>{@code cognome} - cognome</li>
 *   <li>{@code email} - indirizzo email</li>
 *   <li>{@code sesso} - sesso dell'utente</li>
 *   <li>{@code numeroTelefono} - numero di telefono</li>
 *   <li>{@code citta} - città di residenza</li>
 *   <li>{@code dataNascita} - data di nascita</li>
 *   <li>{@code createdAt} - data di creazione dell'account</li>
 * </ul>
 */

public class UserDto {
    
    private Long id;
    private String username;
    private String nome;
    private String cognome;
    private String email;
    private String sesso;
    private String numeroTelefono;
    private String citta;
    private LocalDate dataNascita;
    private LocalDateTime createdAt;
    
    // Costruttori
    public UserDto() { }
    
    public UserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.nome = user.getNome();
        this.cognome = user.getCognome();
        this.email = user.getEmail();
        this.sesso = user.getSesso();
        this.numeroTelefono = user.getNumeroTelefono();
        this.citta = user.getCitta();
        this.dataNascita = user.getDataNascita();
        this.createdAt = user.getCreatedAt();
    }
    
    // Factory method
    public static UserDto fromUser(User user) {
        return new UserDto(user);
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
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
    
    public String getSesso() {
        return sesso;
    }
    
    public void setSesso(String sesso) {
        this.sesso = sesso;
    }
    
    public String getNumeroTelefono() {
        return numeroTelefono;
    }
    
    public void setNumeroTelefono(String numeroTelefono) {
        this.numeroTelefono = numeroTelefono;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}