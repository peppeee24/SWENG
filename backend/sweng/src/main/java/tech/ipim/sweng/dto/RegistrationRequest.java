package tech.ipim.sweng.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationRequest {

    @NotBlank(message = "Username è obbligatorio")
    @Size(min = 3, max = 50, message = "Username deve essere tra 3 e 50 caratteri")
    private String username;

    @NotBlank(message = "Password è obbligatoria")
    @Size(min = 6, message = "Password deve essere almeno 6 caratteri")
    private String password;

    public RegistrationRequest() {}

    public RegistrationRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

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
        return "RegistrationRequest{" +
                "username='" + username + '\'' +
                '}';
    }
}