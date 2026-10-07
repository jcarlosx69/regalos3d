package es.labjc.regalos3d.informe;

import es.labjc.regalos3d.informe.InformeDtos.ImporteAnio;
import es.labjc.regalos3d.informe.InformeDtos.ImporteCliente;
import es.labjc.regalos3d.informe.InformeDtos.ImporteOcasion;
import es.labjc.regalos3d.informe.InformeDtos.ModeloEncargado;
import es.labjc.regalos3d.informe.InformeDtos.Resumen;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Consultas de los informes sobre los importes guardados en {@code encargo}.
 * El año es el de la fecha del encargo. Los alias en snake_case se asignan a los campos de los records.
 */
@Service
public class InformeService {

    private static final String REALIZADO = "('ENTREGADO','COBRADO')";
    private static final String EN_CURSO = "('ACEPTADO','EN_COLA','IMPRIMIENDO','TERMINADO')";

    private final JdbcClient jdbc;

    public InformeService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Resumen resumen(Integer anio) {
        return jdbc.sql("""
                        SELECT
                          COALESCE(SUM(tipo = 'VENTA' AND estado IN %1$s), 0) AS ventas,
                          COALESCE(SUM(CASE WHEN tipo = 'VENTA' AND estado IN %1$s THEN precio_final END), 0) AS facturado,
                          COALESCE(SUM(CASE WHEN tipo = 'VENTA' AND estado = 'COBRADO' THEN precio_final END), 0) AS cobrado,
                          COALESCE(SUM(CASE WHEN tipo = 'VENTA' AND estado = 'ENTREGADO' THEN precio_final END), 0) AS pendiente_cobro,
                          COALESCE(SUM(CASE WHEN tipo = 'VENTA' AND estado IN %1$s THEN precio_final - coste END), 0) AS beneficio,
                          COALESCE(SUM(tipo = 'REGALO' AND estado IN %1$s), 0) AS regalos,
                          COALESCE(SUM(CASE WHEN tipo = 'REGALO' AND estado IN %1$s THEN coste END), 0) AS coste_regalos,
                          COALESCE(SUM(CASE WHEN tipo = 'REGALO' AND estado IN %1$s THEN precio_final END), 0) AS valor_regalos,
                          COALESCE(SUM(estado IN %2$s), 0) AS en_curso,
                          COALESCE(SUM(CASE WHEN estado IN %2$s THEN precio_final END), 0) AS importe_en_curso,
                          COALESCE(SUM(estado = 'PRESUPUESTO'), 0) AS presupuestos,
                          COALESCE(SUM(CASE WHEN estado = 'PRESUPUESTO' THEN precio_final END), 0) AS importe_presupuestos
                        FROM encargo
                        WHERE (:anio IS NULL OR YEAR(fecha) = :anio)""".formatted(REALIZADO, EN_CURSO))
                .param("anio", anio)
                .query(Resumen.class)
                .single();
    }

    public List<ImporteCliente> porCliente(Integer anio) {
        return jdbc.sql("""
                        SELECT c.id AS cliente_id, c.nombre, COUNT(*) AS encargos,
                          COALESCE(SUM(CASE WHEN e.tipo = 'VENTA' THEN e.precio_final END), 0) AS facturado,
                          COALESCE(SUM(CASE WHEN e.tipo = 'VENTA' THEN e.precio_final - e.coste END), 0) AS beneficio,
                          COALESCE(SUM(CASE WHEN e.tipo = 'REGALO' THEN e.coste END), 0) AS coste_regalos
                        FROM encargo e
                        JOIN cliente c ON c.id = e.cliente_id
                        WHERE e.estado IN %s AND (:anio IS NULL OR YEAR(e.fecha) = :anio)
                        GROUP BY c.id, c.nombre
                        ORDER BY facturado DESC, coste_regalos DESC, c.nombre""".formatted(REALIZADO))
                .param("anio", anio)
                .query(ImporteCliente.class)
                .list();
    }

    public List<ImporteAnio> porAnio() {
        return jdbc.sql("""
                        SELECT YEAR(fecha) AS anio,
                          COALESCE(SUM(tipo = 'VENTA'), 0) AS ventas,
                          COALESCE(SUM(CASE WHEN tipo = 'VENTA' THEN precio_final END), 0) AS facturado,
                          COALESCE(SUM(CASE WHEN tipo = 'VENTA' THEN precio_final - coste END), 0) AS beneficio,
                          COALESCE(SUM(tipo = 'REGALO'), 0) AS regalos,
                          COALESCE(SUM(CASE WHEN tipo = 'REGALO' THEN coste END), 0) AS coste_regalos
                        FROM encargo
                        WHERE estado IN %s
                        GROUP BY YEAR(fecha)
                        ORDER BY anio DESC""".formatted(REALIZADO))
                .query(ImporteAnio.class)
                .list();
    }

    public List<ImporteOcasion> porOcasion(Integer anio) {
        return jdbc.sql("""
                        SELECT COALESCE(ocasion, 'Sin ocasión') AS ocasion, COUNT(*) AS encargos,
                          COALESCE(SUM(precio_final), 0) AS importe
                        FROM encargo
                        WHERE estado IN %s AND (:anio IS NULL OR YEAR(fecha) = :anio)
                        GROUP BY COALESCE(ocasion, 'Sin ocasión')
                        ORDER BY importe DESC""".formatted(REALIZADO))
                .param("anio", anio)
                .query(ImporteOcasion.class)
                .list();
    }

    /** Modelos con más unidades en encargos aceptados o posteriores (sin presupuestos ni cancelados). */
    public List<ModeloEncargado> modelos(int limite) {
        return jdbc.sql("""
                        SELECT m.id AS modelo_id, m.nombre, m.manyfold_model_id,
                          SUM(l.cantidad) AS unidades,
                          COUNT(DISTINCT e.id) AS encargos,
                          COUNT(DISTINCT e.cliente_id) AS clientes
                        FROM encargo_linea l
                        JOIN encargo e ON e.id = l.encargo_id
                        JOIN modelo m ON m.id = l.modelo_id
                        WHERE e.estado NOT IN ('PRESUPUESTO', 'CANCELADO')
                        GROUP BY m.id, m.nombre, m.manyfold_model_id
                        ORDER BY unidades DESC, m.nombre
                        LIMIT :limite""")
                .param("limite", limite)
                .query(ModeloEncargado.class)
                .list();
    }
}
