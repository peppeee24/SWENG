package tech.ipim.sweng.dto;

import tech.ipim.sweng.model.Cartella;
import java.time.LocalDateTime;

/**
 * DTO per rappresentare una cartella
 */
public class CartellaDto {
    
    private Long id;
    private String nome;
    private String descrizione;
    private String proprietario;
    private LocalDateTime dataCreazione;
    private LocalDateTime dataModifica;
    private String colore;
    private long numeroNote;
    
    public CartellaDto() {}
    
    public CartellaDto(Cartella cartella) {
        this.id = cartella.getId();
        this.nome = cartella.getNome();
        this.descrizione = cartella.getDescrizione();
        this.proprietario = cartella.getProprietario().getUsername();
        this.dataCreazione = cartella.getDataCreazione();
        this.dataModifica = cartella.getDataModifica();
        this.colore = cartella.getColore();
    }
    
    public static CartellaDto fromCartella(Cartella cartella) {
        return new CartellaDto(cartella);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getDescrizione() { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    
    public String getProprietario() { return proprietario; }
    public void setProprietario(String proprietario) { this.proprietario = proprietario; }
    
    public LocalDateTime getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(LocalDateTime dataCreazione) { this.dataCreazione = dataCreazione; }
    
    public LocalDateTime getDataModifica() { return dataModifica; }
    public void setDataModifica(LocalDateTime dataModifica) { this.dataModifica = dataModifica; }
    
    public String getColore() { return colore; }
    public void setColore(String colore) { this.colore = colore; }
    
    public long getNumeroNote() { return numeroNote; }
    public void setNumeroNote(long numeroNote) { this.numeroNote = numeroNote; }
}