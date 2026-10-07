package es.labjc.regalos3d.documento;

import java.util.List;

/**
 * Lo que necesita una plantilla PDF, con los importes y fechas ya formateados: la plantilla solo pinta texto.
 * {@code null} en un campo opcional significa "no se muestra".
 *
 * @param valorado      si se muestran precios e importes (presupuesto y albarán de una venta)
 * @param validoHasta   solo en el presupuesto
 * @param ajusteConcepto "Descuento" o "Ajuste de precio" cuando el precio final se fijó a mano
 * @param textoPie      texto libre de Ajustes al pie del documento
 */
public record DocumentoVista(
        String titulo,
        String referencia,
        String fecha,
        String validoHasta,
        boolean valorado,
        String ocasion,
        ClienteVista cliente,
        List<LineaVista> lineas,
        int unidades,
        String sumaArticulos,
        String ajusteConcepto,
        String ajuste,
        String total,
        String textoPie) {

    /** {@code direccion} va partida en líneas; el teléfono, ya legible ("611 22 33 44"). */
    public record ClienteVista(String nombre, List<String> direccion, String telefono, String email) {
    }

    /** {@code detalle}: materiales y colores; {@code varios}: "Incluye: …". */
    public record LineaVista(int numero, String nombre, String detalle, String varios, int cantidad,
                             boolean obsequio, String precio, String importe) {
    }
}
