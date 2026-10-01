package edu.unimagdalena.PulsePass.exception;

//Sección 35: se lanza cuando el recurso existe pero la operación no es válida según las reglas de negocio.


public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
