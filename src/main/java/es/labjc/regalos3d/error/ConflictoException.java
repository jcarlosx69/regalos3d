package es.labjc.regalos3d.error;

/** Choque con datos existentes: teléfono o ID de Manyfold repetido, borrar algo con regalos asociados... */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
