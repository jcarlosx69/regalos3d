package es.labjc.regalos3d.encargo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Cálculo del precio de un encargo. Sin estado ni dependencias: se prueba con tests unitarios y el
 * frontal reproduce las mismas reglas (core/calculo.ts) para el presupuesto en vivo.
 *
 * <p>Por unidad de cada línea, redondeando cada componente a céntimos (HALF_UP):</p>
 * <pre>
 *   material      = Σ gramos × €/kg / 1000            (cada bobina redondeada por separado)
 *   energía       = horas impresión × W / 1000 × €/kWh
 *   amortización  = horas impresión × €/h amortización
 *   coste         = material + energía + amortización + varios
 *   mano de obra  = horas mano de obra × €/h            (solo si se incluye)
 *   margen        = (coste + mano de obra) × % / 100    (solo si se incluye)
 *   precio        = coste + mano de obra + margen        (precio calculado)
 *   precio unidad = 0 si es obsequio; si no, el manual del artículo o el calculado
 *   importe línea = precio unidad × cantidad
 * </pre>
 * El encargo suma las líneas; el precio final es el manual si existe o la suma de artículos.
 * Un obsequio (regalo o promoción dentro del encargo) no se cobra, pero su coste sí cuenta.
 */
public final class CalculadoraPrecio {

    private static final BigDecimal MIL = BigDecimal.valueOf(1000);

    private CalculadoraPrecio() {
    }

    public record Tarifas(BigDecimal potenciaW, BigDecimal precioKwh, BigDecimal manoObraHora,
                          BigDecimal amortizacionHora, BigDecimal margenPct,
                          boolean incluirManoObra, boolean incluirMargen) {
    }

    public record Bobina(BigDecimal gramos, BigDecimal precioKg) {
    }

    /** {@code precioManual}: precio por unidad fijado a mano (se ignora si es obsequio). */
    public record Linea(int cantidad, BigDecimal horasImpresion, BigDecimal horasManoObra,
                        BigDecimal variosImporte, List<Bobina> bobinas, boolean obsequio, BigDecimal precioManual) {

        /** Línea con el precio calculado. */
        public Linea(int cantidad, BigDecimal horasImpresion, BigDecimal horasManoObra,
                     BigDecimal variosImporte, List<Bobina> bobinas) {
            this(cantidad, horasImpresion, horasManoObra, variosImporte, bobinas, false, null);
        }
    }

    /**
     * Importes por unidad de una línea, más su importe total. {@code precioCalculado} es el que sale de las
     * tarifas; {@code precioUnitario}, el que se cobra (0 en un obsequio, o el manual si lo hay).
     */
    public record ResultadoLinea(BigDecimal material, BigDecimal energia, BigDecimal amortizacion,
                                 BigDecimal varios, BigDecimal costeUnitario, BigDecimal manoObraUnitaria,
                                 BigDecimal margenUnitario, BigDecimal precioCalculado, BigDecimal precioUnitario,
                                 BigDecimal importe) {
    }

    /** {@code precioCalculado} es la suma de los importes de las líneas (con sus precios manuales y obsequios). */
    public record Resultado(List<ResultadoLinea> lineas, BigDecimal coste, BigDecimal manoObra,
                            BigDecimal margen, BigDecimal precioCalculado, BigDecimal precioFinal) {

        /** Precio final menos coste: lo que se gana en una venta. */
        public BigDecimal beneficio() {
            return precioFinal.subtract(coste);
        }

        /** Lo que se aparta del cálculo por obsequios y precios de artículo a mano (negativo si se descuenta). */
        public BigDecimal ajusteArticulos() {
            return precioCalculado.subtract(coste).subtract(manoObra).subtract(margen);
        }
    }

    public static Resultado calcular(Tarifas t, List<Linea> lineas, BigDecimal precioManual) {
        List<ResultadoLinea> resultados = new ArrayList<>();
        BigDecimal coste = BigDecimal.ZERO.setScale(2);
        BigDecimal manoObra = BigDecimal.ZERO.setScale(2);
        BigDecimal margen = BigDecimal.ZERO.setScale(2);
        BigDecimal total = BigDecimal.ZERO.setScale(2);

        for (Linea l : lineas) {
            ResultadoLinea r = calcularLinea(t, l);
            BigDecimal cantidad = BigDecimal.valueOf(l.cantidad());
            resultados.add(r);
            coste = coste.add(r.costeUnitario().multiply(cantidad));
            manoObra = manoObra.add(r.manoObraUnitaria().multiply(cantidad));
            margen = margen.add(r.margenUnitario().multiply(cantidad));
            total = total.add(r.importe());
        }
        BigDecimal precioFinal = precioManual != null ? centimos(precioManual) : total;
        return new Resultado(List.copyOf(resultados), coste, manoObra, margen, total, precioFinal);
    }

    static ResultadoLinea calcularLinea(Tarifas t, Linea l) {
        BigDecimal horas = nz(l.horasImpresion());

        BigDecimal material = BigDecimal.ZERO.setScale(2);
        for (Bobina b : l.bobinas()) {
            material = material.add(centimos(nz(b.gramos()).multiply(nz(b.precioKg())).divide(MIL)));
        }
        BigDecimal energia = centimos(horas.multiply(nz(t.potenciaW())).divide(MIL).multiply(nz(t.precioKwh())));
        BigDecimal amortizacion = centimos(horas.multiply(nz(t.amortizacionHora())));
        BigDecimal varios = centimos(nz(l.variosImporte()));
        BigDecimal coste = material.add(energia).add(amortizacion).add(varios);

        BigDecimal manoObra = t.incluirManoObra()
                ? centimos(nz(l.horasManoObra()).multiply(nz(t.manoObraHora())))
                : BigDecimal.ZERO.setScale(2);
        BigDecimal base = coste.add(manoObra);
        BigDecimal margen = t.incluirMargen()
                ? centimos(base.multiply(nz(t.margenPct())).movePointLeft(2))
                : BigDecimal.ZERO.setScale(2);
        BigDecimal calculado = base.add(margen);
        BigDecimal precio = l.obsequio() ? BigDecimal.ZERO.setScale(2)
                : l.precioManual() != null ? centimos(l.precioManual())
                : calculado;
        BigDecimal importe = precio.multiply(BigDecimal.valueOf(l.cantidad()));

        return new ResultadoLinea(material, energia, amortizacion, varios, coste, manoObra, margen, calculado,
                precio, importe);
    }

    static BigDecimal centimos(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
