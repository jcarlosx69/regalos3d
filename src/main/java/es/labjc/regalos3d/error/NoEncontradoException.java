package es.labjc.regalos3d.error;

public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String entidad, Object id) {
        super(entidad + " " + id + " no existe");
    }
}
