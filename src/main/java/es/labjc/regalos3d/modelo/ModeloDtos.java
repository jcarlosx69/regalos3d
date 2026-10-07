package es.labjc.regalos3d.modelo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class ModeloDtos {

    private ModeloDtos() {
    }

    /** El ID de Manyfold se copia a mano de la URL del modelo hasta la integración de la v2. */
    public record ModeloRequest(
            @NotBlank @Size(max = 64) String manyfoldModelId,
            @NotBlank @Size(max = 150) String nombre,
            @DecimalMin("0.0") @Digits(integer = 5, fraction = 2) BigDecimal gramosEstimados) {
    }

    public record ModeloResponse(Long id, String manyfoldModelId, String nombre,
                                 BigDecimal gramosEstimados, String manyfoldUrl) {

        static ModeloResponse de(Modelo m, String urlBase) {
            return new ModeloResponse(m.getId(), m.getManyfoldModelId(), m.getNombre(),
                    m.getGramosEstimados(), urlBase + "/models/" + m.getManyfoldModelId());
        }
    }
}
