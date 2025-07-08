package tech.ipim.sweng.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCartellaRequest {

    @NotBlank(message = "Nome cartella è obbligatorio")
    @Size(min = 1, max = 100, message = "Nome cartella deve essere tra 1 e 100 caratteri")
    private String nome;

    @Size(max = 500, message = "Descrizione deve essere massimo 500 caratteri")
    private String descrizione;

    @Size(max = 7, message = "Colore deve essere un codice hex valido")
    private String colore;

    public CreateCartellaRequest() { }

    public CreateCartellaRequest(String nome) {
        this.nome = nome;
    }

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