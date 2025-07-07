// RestoreVersionRequest.java
package tech.ipim.sweng.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RestoreVersionRequest {

    @NotNull(message = "Il numero di versione è obbligatorio")
    @Min(value = 1, message = "Il numero di versione deve essere almeno 1")
    private Integer versionNumber;

    public RestoreVersionRequest() {}

    public RestoreVersionRequest(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }
}


