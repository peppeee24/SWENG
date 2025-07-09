package tech.ipim.sweng.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
/**
 * DTO per la richiesta di registrazione di un nuovo utente.
 * <p>
 * Contiene i dati necessari per creare un account, con validazioni
 * sulle proprietà per garantire correttezza e sicurezza.
 * <p>
 * Campi obbligatori:
 * <ul>
 *   <li>{@code username} - obbligatorio, 3-50 caratteri</li>
 *   <li>{@code password} - obbligatoria, almeno 6 caratteri</li>
 * </ul>
 * <p>
 * Campi opzionali (con limiti di lunghezza e pattern):
 * <ul>
 *   <li>{@code nome} e {@code cognome} - massimo 100 caratteri</li>
 *   <li>{@code email} - formato email valido, max 150 caratteri</li>
 *   <li>{@code sesso} - deve essere "M", "F" o "ALTRO"</li>
 *   <li>{@code numeroTelefono} - formato numerico internazionale</li>
 *   <li>{@code citta} - massimo 100 caratteri</li>
 *   <li>{@code dataNascita} - data di nascita (senza validazioni aggiuntive)</li>
 * </ul>
 * <p>
 * Il metodo {@code toString()} omette password per motivi di sicurezza.
 */
public class RegistrationRequest {

    @NotBlank(message = "Username è obbligatorio")
    @Size(min = 3, max = 50, message = "Username deve essere tra 3 e 50 caratteri")
    private String username;

    @NotBlank(message = "Password è obbligatoria")
    @Size(min = 6, message = "Password deve essere almeno 6 caratteri")
    private String password;

    //  campi opzionali per la registrazione
    @Size(max = 100, message = "Nome deve essere massimo 100 caratteri")
    private String nome;

    @Size(max = 100, message = "Cognome deve essere massimo 100 caratteri")
    private String cognome;

    @Email(message = "Email deve essere valida")
    @Size(max = 150, message = "Email deve essere massimo 150 caratteri")
    private String email;

    @Pattern(regexp = "^(M|F|ALTRO)$", message = "Sesso deve essere M, F o ALTRO")
    private String sesso;

    @Pattern(regexp = "^[\\+]?[0-9\\s\\-\\(\\)]{8,20}$", message = "Numero di telefono non valido")
    private String numeroTelefono;

    @Size(max = 100, message = "Città deve essere massimo 100 caratteri")
    private String citta;

    private LocalDate dataNascita;

    // Costruttori
    public RegistrationRequest() { }

    public RegistrationRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters e Setters esistenti
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

    //  Getters e Setters
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

    @Override
    public String toString() {
        return "RegistrationRequest{"
                + "username='" + username + '\''
                + ", nome='" + nome + '\''
                + ", cognome='" + cognome + '\''
                + ", email='" + email + '\''
                + ", citta='" + citta + '\''
                + '}';
    }
}