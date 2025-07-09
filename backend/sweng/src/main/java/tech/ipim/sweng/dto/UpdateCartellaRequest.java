package tech.ipim.sweng.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * DTO per la richiesta di aggiornamento di una cartella.
 * <p>
 * Contiene i dati aggiornabili della cartella, con le relative validazioni.
 * <p>
 * Campi:
 * <ul>
 *   <li>{@code nome} - obbligatorio, lunghezza tra 1 e 100 caratteri</li>
 *   <li>{@code descrizione} - opzionale, massimo 500 caratteri</li>
 *   <li>{@code colore} - opzionale, codice colore esadecimale massimo 7 caratteri (es. #FFFFFF)</li>
 * </ul>
 */

public class UpdateCartellaRequest {

    @NotBlank(message = "Nome cartella è obbligatorio")
    @Size(min = 1, max = 100, message = "Nome cartella deve essere tra 1 e 100 caratteri")
    private String nome;

    @Size(max = 500, message = "Descrizione deve essere massimo 500 caratteri")
    private String descrizione;

    @Size(max = 7, message = "Colore deve essere un codice hex valido")
    private String colore;

    public UpdateCartellaRequest() { }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getColore() {
        return colore;
    }

    public void setColore(String colore) {
        this.colore = colore;
    }
}