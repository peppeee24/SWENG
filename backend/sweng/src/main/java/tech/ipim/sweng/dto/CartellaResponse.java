

package tech.ipim.sweng.dto;

/**
 * DTO di risposta per operazioni relative a una cartella.
 * <p>
 * Utilizzato per restituire l'esito di un'operazione (successo o errore) insieme a un messaggio
 * e, opzionalmente, ai dati della cartella coinvolta.
 * <p>
 * Campi principali:
 * <ul>
 *   <li>{@code success} - indica se l'operazione è andata a buon fine</li>
 *   <li>{@code message} - messaggio descrittivo del risultato</li>
 *   <li>{@code cartella} - DTO della cartella, incluso solo quando necessario</li>
 * </ul>
 * <p>
 * Fornisce metodi statici factory per costruire rapidamente risposte di successo o errore:
 * <ul>
 *   <li>{@code success(String message)}</li>
 *   <li>{@code success(String message, CartellaDto cartella)}</li>
 *   <li>{@code error(String message)}</li>
 * </ul>
 */
public class CartellaResponse {

    private boolean success;
    private String message;
    private CartellaDto cartella;

    public CartellaResponse() { }

    public CartellaResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public CartellaResponse(boolean success, String message, CartellaDto cartella) {
        this.success = success;
        this.message = message;
        this.cartella = cartella;
    }

    public static CartellaResponse success(String message, CartellaDto cartella) {
        return new CartellaResponse(true, message, cartella);
    }

    public static CartellaResponse success(String message) {
        return new CartellaResponse(true, message);
    }

    public static CartellaResponse error(String message) {
        return new CartellaResponse(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CartellaDto getCartella() {
        return cartella;
    }

    public void setCartella(CartellaDto cartella) {
        this.cartella = cartella;
    }
}