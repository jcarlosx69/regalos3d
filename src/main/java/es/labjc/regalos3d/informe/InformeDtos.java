package es.labjc.regalos3d.informe;

import java.math.BigDecimal;

/**
 * Importes de los informes. "Realizados" son los encargos entregados o cobrados; el beneficio es
 * precio final menos coste (material, energía, amortización y varios).
 */
public final class InformeDtos {

    private InformeDtos() {
    }

    public record Resumen(Long ventas, BigDecimal facturado, BigDecimal cobrado, BigDecimal pendienteCobro,
                          BigDecimal beneficio, Long regalos, BigDecimal costeRegalos, BigDecimal valorRegalos,
                          Long enCurso, BigDecimal importeEnCurso, Long presupuestos,
                          BigDecimal importePresupuestos) {
    }

    public record ImporteCliente(Long clienteId, String nombre, Long encargos, BigDecimal facturado,
                                 BigDecimal beneficio, BigDecimal costeRegalos) {
    }

    public record ImporteAnio(Integer anio, Long ventas, BigDecimal facturado, BigDecimal beneficio,
                              Long regalos, BigDecimal costeRegalos) {
    }

    public record ImporteOcasion(String ocasion, Long encargos, BigDecimal importe) {
    }

    public record ModeloEncargado(Long modeloId, String nombre, String manyfoldModelId, Long unidades,
                                  Long encargos, Long clientes) {
    }
}
