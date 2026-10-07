package es.labjc.regalos3d.documento;

import es.labjc.regalos3d.ajustes.AjustesService;
import es.labjc.regalos3d.cliente.Cliente;
import es.labjc.regalos3d.documento.DocumentoVista.ClienteVista;
import es.labjc.regalos3d.documento.DocumentoVista.LineaVista;
import es.labjc.regalos3d.encargo.Encargo;
import es.labjc.regalos3d.encargo.EncargoLinea;
import es.labjc.regalos3d.encargo.EncargoRepository;
import es.labjc.regalos3d.encargo.TipoEncargo;
import es.labjc.regalos3d.error.ConflictoException;
import es.labjc.regalos3d.error.NoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Presupuesto, albarán y etiqueta de un encargo. Los documentos no se guardan: se generan al pedirlos con los
 * datos actuales del encargo. Las notas internas del encargo nunca aparecen en ellos.
 */
@Service
@Transactional(readOnly = true)
public class DocumentoService {

    /** PDF generado y el nombre de archivo con el que se ofrece ("Presupuesto E-2026-0006.pdf"). */
    public record Pdf(String nombre, byte[] contenido) {
    }

    private final EncargoRepository encargos;
    private final AjustesService ajustes;
    private final MotorPdf motor;
    private final Clock clock;

    public DocumentoService(EncargoRepository encargos, AjustesService ajustes, MotorPdf motor, Clock clock) {
        this.encargos = encargos;
        this.ajustes = ajustes;
        this.motor = motor;
        this.clock = clock;
    }

    /** Solo para ventas. Fecha de hoy y validez según Ajustes. */
    public Pdf presupuesto(long id) {
        Encargo e = cargar(id);
        if (e.getTipo() != TipoEncargo.VENTA) {
            throw new ConflictoException("Un regalo no lleva presupuesto: usa el albarán o la etiqueta");
        }
        var docs = ajustes.documentos();
        LocalDate hoy = LocalDate.now(clock);
        var vista = vista(e, "Presupuesto", hoy, hoy.plusDays(docs.validezPresupuestoDias()), true, docs.textoPie());
        return new Pdf(archivo("Presupuesto", e), motor.pdf("presupuesto", vista));
    }

    /** Valorado en las ventas y sin precios en los regalos. Fecha de entrega, o la de hoy si aún no se entregó. */
    public Pdf albaran(long id) {
        Encargo e = cargar(id);
        boolean valorado = e.getTipo() == TipoEncargo.VENTA;
        var vista = vista(e, "Albarán", fechaEntrega(e), null, valorado, ajustes.documentos().textoPie());
        return new Pdf(archivo("Albarán", e), motor.pdf("albaran", vista));
    }

    /** Etiqueta A6 para el paquete: destinatario y contenido, sin precios. */
    public Pdf etiqueta(long id) {
        Encargo e = cargar(id);
        var vista = vista(e, "Etiqueta", fechaEntrega(e), null, false, null);
        return new Pdf(archivo("Etiqueta", e), motor.pdf("etiqueta", vista));
    }

    // ------------------------------------------------------------------ vista

    static DocumentoVista vista(Encargo e, String titulo, LocalDate fecha, LocalDate validoHasta, boolean valorado,
                                String textoPie) {
        Cliente c = e.getCliente();
        var cliente = new ClienteVista(c.getNombre(), Formato.lineas(c.getDireccion()), Formato.telefono(c.getTelefono()),
                Formato.vacioANull(c.getEmail()));

        List<LineaVista> lineas = new ArrayList<>();
        int numero = 1;
        int unidades = 0;
        for (EncargoLinea l : e.getLineas()) {
            lineas.add(new LineaVista(numero++, l.getModelo().getNombre(), materiales(l), varios(l), l.getCantidad(),
                    l.isObsequio(), Formato.euros(l.getPrecioUnitario()), Formato.euros(l.getImporte())));
            unidades += l.getCantidad();
        }

        // Precio final fijado a mano: se muestra la diferencia con la suma de artículos
        String concepto = null;
        String ajuste = null;
        if (e.getPrecioManual() != null) {
            BigDecimal diferencia = e.getPrecioFinal().subtract(e.getPrecioCalculado());
            if (diferencia.signum() != 0) {
                concepto = diferencia.signum() < 0 ? "Descuento" : "Ajuste de precio";
                ajuste = Formato.euros(diferencia);
            }
        }

        return new DocumentoVista(titulo, e.getReferencia(), Formato.fecha(fecha), Formato.fecha(validoHasta),
                valorado, Formato.vacioANull(e.getOcasion()), cliente, List.copyOf(lineas), unidades,
                Formato.euros(e.getPrecioCalculado()), concepto, ajuste, Formato.euros(e.getPrecioFinal()),
                Formato.vacioANull(textoPie));
    }

    /** "PLA Blanco · PLA Rosa", sin repetir combinaciones. */
    private static String materiales(EncargoLinea l) {
        String texto = l.getFilamentos().stream()
                .map(f -> f.getColor() == null || f.getColor().isBlank() ? f.getMaterial() : f.getMaterial() + " " + f.getColor())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(" · "));
        return texto.isEmpty() ? null : texto;
    }

    private static String varios(EncargoLinea l) {
        String concepto = Formato.vacioANull(l.getVariosConcepto());
        return concepto == null ? null : "Incluye: " + concepto;
    }

    private LocalDate fechaEntrega(Encargo e) {
        return e.getFechaEntrega() != null ? e.getFechaEntrega() : LocalDate.now(clock);
    }

    private static String archivo(String tipo, Encargo e) {
        return tipo + " " + e.getReferencia() + ".pdf";
    }

    private Encargo cargar(long id) {
        return encargos.findConDetalleById(id).orElseThrow(() -> new NoEncontradoException("Encargo", id));
    }
}
