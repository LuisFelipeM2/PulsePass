package edu.unimagdalena.PulsePass.exception;


//Sección 35: se lanza cuando el recurso solicitado no existe.

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " not found: " + identifier);
    }
}
