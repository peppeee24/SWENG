package tech.ipim.sweng.dto;

import tech.ipim.sweng.model.TipoPermesso;
import java.util.List;
/**
 * DTO per la gestione dei permessi di accesso a una risorsa.
 * <p>
 * Contiene il tipo di permesso (es. PRIVATO, CONDIVISO) e le liste
 * degli utenti che hanno permessi di lettura e scrittura.
 * <p>
 * Utilizzato per trasferire informazioni sui permessi tra client e server.
 */

public class PermissionDto {

    private TipoPermesso tipoPermesso;
    private List<String> utentiLettura;
    private List<String> utentiScrittura;

    public PermissionDto() { }

    public PermissionDto(TipoPermesso tipoPermesso, List<String> utentiLettura, List<String> utentiScrittura) {
        this.tipoPermesso = tipoPermesso;
        this.utentiLettura = utentiLettura;
        this.utentiScrittura = utentiScrittura;
    }

    public TipoPermesso getTipoPermesso() {
        return tipoPermesso;
    }

    public void setTipoPermesso(TipoPermesso tipoPermesso) {
        this.tipoPermesso = tipoPermesso;
    }

    public List<String> getUtentiLettura() {
        return utentiLettura;
    }

    public void setUtentiLettura(List<String> utentiLettura) {
        this.utentiLettura = utentiLettura;
    }

    public List<String> getUtentiScrittura() {
        return utentiScrittura;
    }

    public void setUtentiScrittura(List<String> utentiScrittura) {
        this.utentiScrittura = utentiScrittura;
    }
}