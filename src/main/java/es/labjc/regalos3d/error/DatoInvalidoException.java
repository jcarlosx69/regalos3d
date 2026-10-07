package es.labjc.regalos3d.error;

/** Dato de entrada que pasa la validación de formato pero no la regla de negocio (p. ej. un móvil no español). */
public class DatoInvalidoException extends RuntimeException {

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
