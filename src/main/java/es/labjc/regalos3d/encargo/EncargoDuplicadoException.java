package es.labjc.regalos3d.encargo;

import es.labjc.regalos3d.encargo.EncargoDtos.EncargoPrevio;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * El cliente ya recibió alguno de los modelos de un regalo. No es un error: pide confirmación.
 * El cliente repite la petición con {@code ?confirmarDuplicado=true} para guardarlo igualmente.
 */
public class EncargoDuplicadoException extends RuntimeException {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<EncargoPrevio> previos;

    public EncargoDuplicadoException(String cliente, List<EncargoPrevio> previos) {
        super(mensaje(cliente, previos));
        this.previos = List.copyOf(previos);
    }

    public List<EncargoPrevio> getPrevios() {
        return previos;
    }

    private static String mensaje(String cliente, List<EncargoPrevio> previos) {
        Set<String> modelos = new LinkedHashSet<>();
        previos.forEach(p -> modelos.add(p.modelo()));
        EncargoPrevio ultimo = previos.getFirst();
        return cliente + " ya recibió " + String.join(", ", modelos) + " (último: " + ultimo.referencia()
                + " del " + FECHA.format(ultimo.fecha()) + "). Repite con confirmarDuplicado=true para guardarlo igualmente.";
    }
}
