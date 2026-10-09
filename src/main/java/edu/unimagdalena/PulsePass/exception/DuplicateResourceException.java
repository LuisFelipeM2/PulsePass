package edu.unimagdalena.PulsePass.exception;


//Sección 35: se lanza cuando hay un conflicto de unicidad.

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
