package tech.ipim.sweng.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO per la richiesta di login dell’utente.
 * <p>
 * Utilizzato per raccogliere le credenziali dell’utente dal client e inviarle al backend
 * per l'autenticazione.
 * <p>
 * Campi validati:
 * <ul>
 *   <li>{@code username} - obbligatorio, da 3 a 50 caratteri</li>
 *   <li>{@code password} - obbligatoria, minimo 6 caratteri</li>
 * </ul>
 * <p>
 * Nota: la password non viene inclusa nel metodo {@code toString()} per motivi di sicurezza.
 */
public class LoginRequest {

    @NotBlank(message = "Username è obbligatorio")
    @Size(min = 3, max = 50, message = "Username deve essere tra 3 e 50 caratteri")
    private String username;

    @NotBlank(message = "Password è obbligatoria")
    @Size(min = 6, message = "Password deve essere almeno 6 caratteri")
    private String password;

    // Costruttori
    public LoginRequest() { }

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters e Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginRequest{"
                + "username='" + username + '\''
                + '}'; // Non loggare la password!
    }
}