package es.labjc.regalos3d.ajustes;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class AjustesService {

    /** Tarifas del taller. Los encargos copian estos valores al crearse. */
    public record Tarifas(
            @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal precioKg,
            @NotNull @Positive @Digits(integer = 2, fraction = 4) BigDecimal precioKwh,
            @NotNull @Positive @Digits(integer = 5, fraction = 1) BigDecimal potenciaW,
            @NotNull @DecimalMin("0.0") @Digits(integer = 4, fraction = 2) BigDecimal manoObraHora,
            @NotNull @DecimalMin("0.0") @Digits(integer = 4, fraction = 2) BigDecimal amortizacionHora,
            @NotNull @DecimalMin("0.0") @DecimalMax("999.99") @Digits(integer = 3, fraction = 2) BigDecimal margenPct) {

        static Tarifas de(Ajustes a) {
            return new Tarifas(a.getPrecioKg(), a.getPrecioKwh(), a.getPotenciaW(), a.getManoObraHora(),
                    a.getAmortizacionHora(), a.getMargenPct());
        }
    }

    /** Ajustes de los PDF: validez del presupuesto y texto libre al pie (vacío: sin texto). */
    public record Documentos(
            @NotNull @Min(1) @Max(365) Integer validezPresupuestoDias,
            @Size(max = 500) String textoPie) {

        static Documentos de(Ajustes a) {
            return new Documentos(a.getValidezPresupuestoDias(), a.getTextoPie());
        }
    }

    private final AjustesRepository repo;

    public AjustesService(AjustesRepository repo) {
        this.repo = repo;
    }

    public Tarifas tarifas() {
        return Tarifas.de(cargar());
    }

    @Transactional
    public Tarifas guardar(Tarifas t) {
        Ajustes a = cargar();
        a.setPrecioKg(t.precioKg());
        a.setPrecioKwh(t.precioKwh());
        a.setPotenciaW(t.potenciaW());
        a.setManoObraHora(t.manoObraHora());
        a.setAmortizacionHora(t.amortizacionHora());
        a.setMargenPct(t.margenPct());
        return Tarifas.de(a);
    }

    public Documentos documentos() {
        return Documentos.de(cargar());
    }

    @Transactional
    public Documentos guardarDocumentos(Documentos d) {
        Ajustes a = cargar();
        a.setValidezPresupuestoDias(d.validezPresupuestoDias());
        a.setTextoPie(d.textoPie() == null || d.textoPie().isBlank() ? null : d.textoPie().strip());
        return Documentos.de(a);
    }

    private Ajustes cargar() {
        return repo.findById(Ajustes.ID)
                .orElseThrow(() -> new IllegalStateException("Falta la fila de ajustes (migración V4)"));
    }
}
