package es.labjc.regalos3d.documento;

import es.labjc.regalos3d.documento.DocumentoVista.ClienteVista;
import es.labjc.regalos3d.documento.DocumentoVista.LineaVista;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Genera los PDF de verdad (plantillas + fuentes + openhtmltopdf) y lee su texto con PDFBox. Sin Spring ni BD. */
class MotorPdfTest {

    private static final MotorPdf MOTOR = new MotorPdf();
    private static final float MM = 72f / 25.4f;

    private static final ClienteVista TALLER = new ClienteVista("Taller Demo S.L.",
            List.of("C/ Inventada 5, nave 3", "35008 Las Palmas de Gran Canaria"), "611 22 33 44", "compras@taller-demo.example");

    private static DocumentoVista venta(String titulo, String validoHasta) {
        return new DocumentoVista(titulo, "E-2026-0006", "06/10/2026", validoHasta, true, null, TALLER,
                List.of(new LineaVista(1, "Llavero personalizado", "PLA Negro", null, 10, false, "2,50 €", "25,00 €"),
                        new LineaVista(2, "Lámpara luna", "PLA Blanco", "Incluye: tira LED", 1, false, "38,40 €", "38,40 €"),
                        new LineaVista(3, "Maceta geométrica", "PETG Gris", null, 1, true, "0,00 €", "0,00 €")),
                12, "63,40 €", "Descuento", "−3,40 €", "60,00 €", "Pago en efectivo o Bizum.");
    }

    private static DocumentoVista regalo(String titulo) {
        var lucia = new ClienteVista("Lucía Prueba", List.of("C/ Inventada 12, 2º B"), "622 00 00 01", null);
        return new DocumentoVista(titulo, "E-2026-0002", "12/03/2026", null, false, "Cumpleaños", lucia,
                List.of(new LineaVista(1, "Cute rabbit", "PLA Blanco · PLA Rosa", null, 1, false, "1,73 €", "1,73 €")),
                1, "1,73 €", null, null, "1,73 €", null);
    }

    private static String texto(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            // espacios fijos como espacios normales para comparar con comodidad
            return new PDFTextStripper().getText(doc).replace(' ', ' ');
        }
    }

    @Test
    void presupuestoConObsequioDescuentoYPie() throws IOException {
        byte[] pdf = MOTOR.pdf("presupuesto", venta("Presupuesto", "05/11/2026"));

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(texto(pdf))
                .contains("E-2026-0006", "Taller Demo S.L.", "611 22 33 44", "05/11/2026")
                .contains("Llavero personalizado", "Incluye: tira LED", "Obsequio")
                .contains("63,40 €", "Descuento", "60,00 €", "Pago en efectivo o Bizum.");
    }

    @Test
    void albaranDeRegaloSinPreciosConOcasionYFirma() throws IOException {
        String t = texto(MOTOR.pdf("albaran", regalo("Albarán")));

        assertThat(t).contains("E-2026-0002", "Lucía Prueba", "Cute rabbit", "Cumpleaños", "Recibido por");
        assertThat(t).doesNotContain("€");
    }

    @Test
    void albaranDeVentaValorado() throws IOException {
        String t = texto(MOTOR.pdf("albaran", venta("Albarán", null)));

        assertThat(t).contains("25,00 €", "60,00 €", "Recibido por");
    }

    @Test
    void etiquetaA6SinTelefonoNiPrecios() throws IOException {
        byte[] pdf = MOTOR.pdf("etiqueta", regalo("Etiqueta"));

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            var caja = doc.getPage(0).getMediaBox();
            assertThat(caja.getWidth()).isCloseTo(105 * MM, within(1f));
            assertThat(caja.getHeight()).isCloseTo(148 * MM, within(1f));
        }
        String t = texto(pdf);
        assertThat(t).contains("Lucía Prueba", "C/ Inventada 12, 2º B", "Cute rabbit");
        assertThat(t).doesNotContain("622 00 00 01").doesNotContain("€");
    }

    @Test
    void losDatosSeEscapanEnElHtml() {
        var raro = new ClienteVista("<b>Ana</b> & Cía", List.of(), null, null);
        var d = new DocumentoVista("Etiqueta", "E-2026-0001", "01/01/2026", null, false, null, raro, List.of(), 0,
                "0,00 €", null, null, "0,00 €", null);

        assertThat(MOTOR.html("etiqueta", d)).contains("&lt;b&gt;Ana&lt;/b&gt; &amp; Cía").doesNotContain("<b>Ana");
    }

    @Test
    void unPresupuestoLargoOcupaVariasPaginas() throws IOException {
        var lineas = java.util.stream.IntStream.rangeClosed(1, 40)
                .mapToObj(i -> new LineaVista(i, "Pieza " + i, "PLA Negro", null, 1, false, "3,00 €", "3,00 €"))
                .toList();
        var base = venta("Presupuesto", "05/11/2026");
        var d = new DocumentoVista(base.titulo(), base.referencia(), base.fecha(), base.validoHasta(), true, null,
                base.cliente(), lineas, 40, "120,00 €", null, null, "120,00 €", null);

        try (PDDocument doc = Loader.loadPDF(MOTOR.pdf("presupuesto", d))) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }
}
