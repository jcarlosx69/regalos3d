package es.labjc.regalos3d.cliente;

import es.labjc.regalos3d.error.DatoInvalidoException;

import java.util.regex.Pattern;

/**
 * Normaliza móviles españoles al formato {@code +34XXXXXXXXX}.
 * Acepta espacios, guiones, puntos y paréntesis, y los prefijos +34, 0034 o 34.
 */
public final class Telefonos {

    private static final Pattern SEPARADORES = Pattern.compile("[\\s\\-.()]");
    private static final Pattern MOVIL_ES = Pattern.compile("[67]\\d{8}");

    private Telefonos() {
    }

    public static String normalizar(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new DatoInvalidoException("El teléfono es obligatorio");
        }
        String n = SEPARADORES.matcher(entrada.trim()).replaceAll("");

        if (n.startsWith("+34")) {
            n = n.substring(3);
        } else if (n.startsWith("0034")) {
            n = n.substring(4);
        } else if (n.startsWith("34") && n.length() == 11) {
            n = n.substring(2);
        }

        if (!MOVIL_ES.matcher(n).matches()) {
            throw new DatoInvalidoException(
                    "'" + entrada + "' no es un móvil español válido (9 dígitos que empiezan por 6 o 7)");
        }
        return "+34" + n;
    }
}
