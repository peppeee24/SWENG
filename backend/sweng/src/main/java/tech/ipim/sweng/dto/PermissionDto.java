package tech.ipim.sweng.dto;

import tech.ipim.sweng.model.Note.TipoPermesso;
import java.util.Set;

public class PermissionDto {

    private TipoPermesso tipoPermesso;
    private Set<String> utentiLettura;
    private Set<String> utentiScrittura;

    public PermissionDto() {}

    public PermissionDto(TipoPermesso tipoPermesso, Set<String> utentiLettura, Set<String> utentiScrittura) {
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

    public Set<String> getUtentiLettura() {
        return utentiLettura;
    }

    public void setUtentiLettura(Set<String> utentiLettura) {
        this.utentiLettura = utentiLettura;
    }

    public Set<String> getUtentiScrittura() {
        return utentiScrittura;
    }

    public void setUtentiScrittura(Set<String> utentiScrittura) {
        this.utentiScrittura = utentiScrittura;
    }
}