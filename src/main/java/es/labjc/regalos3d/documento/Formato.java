package es.labjc.regalos3d.documento;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Formato español para los documentos: "1.234,50 €", "06/10/2026", "611 22 33 44". */
final class Formato {

    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", ES);

    private Formato() {
    }

    /** Con separador de miles siempre, signo menos tipográfico y espacio fijo antes del símbolo. */
    static String euros(BigDecimal v) {
        // DecimalFormat no es seguro entre hilos: uno por llamada
        var f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(ES));
        String texto = f.format(v == null ? BigDecimal.ZERO : v);
        if (texto.startsWith("-")) {
            texto = "−" + texto.substring(1);
        }
        return texto + " €";
    }

    static String fecha(LocalDate d) {
        return d == null ? null : FECHA.format(d);
    }

    /** Móviles españoles en grupos (611 22 33 44); el resto, tal cual se guardó. */
    static String telefono(String t) {
        if (t == null || t.isBlank()) {
            return null;
        }
        if (t.matches("\\+34\\d{9}")) {
            String n = t.substring(3);
            return n.substring(0, 3) + " " + n.substring(3, 5) + " " + n.substring(5, 7) + " " + n.substring(7);
        }
        return t;
    }

    /** Dirección escrita en varias líneas en el formulario del cliente. */
    static List<String> lineas(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split("\\R")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
