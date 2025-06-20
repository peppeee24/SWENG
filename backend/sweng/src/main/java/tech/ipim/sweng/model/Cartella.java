package tech.ipim.sweng.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cartelle")
public class Cartella {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Nome cartella è obbligatorio")
    @Size(min = 1, max = 100, message = "Nome cartella deve essere tra 1 e 100 caratteri")
    private String nome;

    @Size(max = 500, message = "Descrizione deve essere massimo 500 caratteri")
    @Column(length = 500)
    private String descrizione;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietario_id", nullable = false)
    private User proprietario;

    @Column(name = "data_creazione")
    private LocalDateTime dataCreazione;

    @Column(name = "data_modifica")
    private LocalDateTime dataModifica;

    @Column(name = "colore")
    @Size(max = 7, message = "Colore deve essere un codice hex valido")
    private String colore = "#667eea"; // Colore di default


    public Cartella() {
        this.dataCreazione = LocalDateTime.now();
        this.dataModifica = LocalDateTime.now();
    }

    public Cartella(String nome, User proprietario) {
        this();
        this.nome = nome;
        this.proprietario = proprietario;
    }

    @PreUpdate
    public void preUpdate() {
        this.dataModifica = LocalDateTime.now();
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

    public User getProprietario() {
        return proprietario;
    }

    public void setProprietario(User proprietario) {
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

    public boolean isProprietario(String username) {
        return this.proprietario != null && this.proprietario.getUsername().equals(username);
    }

    @Override
    public String toString() {
        return "Cartella{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", proprietario=" + (proprietario != null ? proprietario.getUsername() : "null") +
                ", dataCreazione=" + dataCreazione +
                '}';
    }
}