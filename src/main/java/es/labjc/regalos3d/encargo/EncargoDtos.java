package es.labjc.regalos3d.encargo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class EncargoDtos {

    private EncargoDtos() {
    }

    // ------------------------------------------------------------------ entrada

    /**
     * Alta o modificación de un encargo. Las tarifas que lleguen vacías se toman de los ajustes actuales
     * (en un alta) o se mantienen (en una modificación). {@code estado} vacío en un alta: PRESUPUESTO para
     * ventas y EN_COLA para regalos; en una modificación, se mantiene.
     */
    public record EncargoRequest(
            @NotNull Long clienteId,
            @NotNull TipoEncargo tipo,
            EstadoEncargo estado,
            @NotNull LocalDate fecha,
            @Size(max = 100) String ocasion,
            @Size(max = 1000) String notas,
            boolean incluirManoObra,
            boolean incluirMargen,
            @Positive @Digits(integer = 5, fraction = 1) BigDecimal potenciaW,
            @Positive @Digits(integer = 2, fraction = 4) BigDecimal precioKwh,
            @DecimalMin("0.0") @Digits(integer = 4, fraction = 2) BigDecimal manoObraHora,
            @DecimalMin("0.0") @Digits(integer = 4, fraction = 2) BigDecimal amortizacionHora,
            @DecimalMin("0.0") @DecimalMax("999.99") @Digits(integer = 3, fraction = 2) BigDecimal margenPct,
            @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal precioManual,
            @NotEmpty(message = "El encargo necesita al menos un artículo") @Valid List<LineaRequest> lineas) {
    }

    /**
     * Un artículo. Horas, gramos, varios y precio manual son por unidad. {@code obsequio}: regalo o promoción
     * dentro del encargo, a 0 € (su coste sí cuenta). {@code precioManual} vacío: el precio calculado.
     */
    public record LineaRequest(
            @NotNull Long modeloId,
            @NotNull @Min(1) @Max(9999) Integer cantidad,
            @DecimalMin("0.0") @Digits(integer = 4, fraction = 2) BigDecimal horasImpresion,
            @DecimalMin("0.0") @Digits(integer = 3, fraction = 2) BigDecimal horasManoObra,
            @Size(max = 150) String variosConcepto,
            @DecimalMin("0.0") @Digits(integer = 6, fraction = 2) BigDecimal variosImporte,
            @Valid List<FilamentoRequest> filamentos,
            boolean obsequio,
            @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal precioManual) {

        public List<FilamentoRequest> filamentosONada() {
            return filamentos == null ? List.of() : filamentos;
        }
    }

    /** {@code precioKg} vacío: se aplica el precio de bobina de los ajustes. */
    public record FilamentoRequest(
            @NotBlank @Size(max = 20) String material,
            @Size(max = 40) String color,
            @NotNull @Positive @Digits(integer = 5, fraction = 2) BigDecimal gramos,
            @Positive @Digits(integer = 4, fraction = 2) BigDecimal precioKg) {
    }

    public record CambioEstado(@NotNull EstadoEncargo estado) {
    }

    // ------------------------------------------------------------------ salida

    public record ClienteResumen(Long id, String nombre, String telefono, String email, String direccion) {
    }

    public record ModeloResumen(Long id, String nombre, String manyfoldModelId) {
    }

    public record FilamentoResponse(Long id, String material, String color, BigDecimal gramos, BigDecimal precioKg) {
    }

    /**
     * Importes por unidad (desglose) más el importe de la línea. {@code precioCalculado} sale de las tarifas;
     * {@code precioUnitario} es el que se cobra (0 en un obsequio, o {@code precioManual} si lo hay).
     */
    public record LineaResponse(Long id, int posicion, ModeloResumen modelo, int cantidad,
                                BigDecimal horasImpresion, BigDecimal horasManoObra,
                                String variosConcepto, BigDecimal variosImporte,
                                List<FilamentoResponse> filamentos, boolean obsequio, BigDecimal precioManual,
                                BigDecimal material, BigDecimal energia, BigDecimal amortizacion,
                                BigDecimal costeUnitario, BigDecimal manoObraUnitaria, BigDecimal margenUnitario,
                                BigDecimal precioCalculado, BigDecimal precioUnitario, BigDecimal importe) {
    }

    public record TarifasEncargo(BigDecimal potenciaW, BigDecimal precioKwh, BigDecimal manoObraHora,
                                 BigDecimal amortizacionHora, BigDecimal margenPct) {
    }

    public record EncargoResponse(Long id, String referencia, ClienteResumen cliente, TipoEncargo tipo,
                                  EstadoEncargo estado, LocalDate fecha, LocalDate fechaEntrega,
                                  LocalDate fechaCobro, String ocasion, String notas,
                                  boolean incluirManoObra, boolean incluirMargen, TarifasEncargo tarifas,
                                  List<LineaResponse> lineas, BigDecimal coste, BigDecimal manoObra,
                                  BigDecimal margen, BigDecimal precioCalculado, BigDecimal precioManual,
                                  BigDecimal precioFinal, BigDecimal beneficio) {
    }

    /** Artículo en la lista de encargos: "Llavero personalizado ×10". */
    public record ArticuloResumen(String modelo, int cantidad, boolean obsequio) {
    }

    public record EncargoResumen(Long id, String referencia, Long clienteId, String clienteNombre,
                                 TipoEncargo tipo, EstadoEncargo estado, LocalDate fecha,
                                 LocalDate fechaEntrega, String ocasion, List<ArticuloResumen> articulos,
                                 int unidades, BigDecimal coste, BigDecimal precioFinal, boolean precioAjustado) {
    }

    /** Encargo anterior en el que el cliente ya recibió uno de los modelos. */
    public record EncargoPrevio(Long encargoId, String referencia, LocalDate fecha, TipoEncargo tipo,
                                String ocasion, Long modeloId, String modelo) {
    }

    public record ComprobacionDuplicado(boolean duplicado, List<EncargoPrevio> previos) {
    }
}
