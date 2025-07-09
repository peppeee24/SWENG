package tech.ipim.sweng.exception;
/**
 * Eccezione personalizzata per segnalare che un utente con lo username specificato
 * esiste già nel sistema.
 * <p>
 * Estende RuntimeException e può essere usata per gestire casi di tentativo di
 * registrazione di un utente duplicato.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }

    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}