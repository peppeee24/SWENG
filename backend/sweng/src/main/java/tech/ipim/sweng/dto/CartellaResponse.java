package tech.ipim.sweng.dto;


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