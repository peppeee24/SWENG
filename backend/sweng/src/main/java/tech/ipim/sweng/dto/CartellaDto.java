package tech.ipim.sweng.dto;

import tech.ipim.sweng.model.Cartella;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) per rappresentare una cartella in modo sicuro e compatto,
 * evitando di esporre direttamente l'entità di dominio {@link Cartella}.
 * <p>
 * Questo oggetto viene utilizzato per trasferire dati tra il backend e il frontend,
 * includendo solo le informazioni rilevanti per la visualizzazione e la gestione
 * lato client.
 * <p>
 * Campi principali:
 * <ul>
 *   <li>{@code id} - identificatore univoco della cartella</li>
 *   <li>{@code nome} - nome della cartella</li>
 *   <li>{@code descrizione} - descrizione opzionale</li>
 *   <li>{@code proprietario} - username del creatore della cartella</li>
 *   <li>{@code dataCreazione}, {@code dataModifica} - timestamp di creazione e modifica</li>
 *   <li>{@code colore} - colore associato per uso grafico</li>
 *   <li>{@code numeroNote} - numero di note contenute (settato esternamente)</li>
 * </ul>
 * <p>
 * Include:
 * <ul>
 *   <li>Costruttore vuoto per la serializzazione</li>
 *   <li>Costruttore che accetta un oggetto {@link Cartella}</li>
 *   <li>Metodo statico {@code fromCartella()} per comodità</li>
 * </ul>
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

    public CartellaDto() { }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getProprietario() {
        return proprietario;
    }

    public void setProprietario(String proprietario) {
        this.proprietario = proprietario;
    }

    public LocalDateTime getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(LocalDateTime dataCreazione) {
        this.dataCreazione = dataCreazione;
    }

    public LocalDateTime getDataModifica() {
        return dataModifica;
    }

    public void setDataModifica(LocalDateTime dataModifica) {
        this.dataModifica = dataModifica;
    }

    public String getColore() {
        return colore;
    }

    public void setColore(String colore) {
        this.colore = colore;
    }

    public long getNumeroNote() {
        return numeroNote;
    }

    public void setNumeroNote(long numeroNote) {
        this.numeroNote = numeroNote;
    }
}