package es.labjc.regalos3d.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración propia de la app (prefijo {@code regalos}).
 * Se valida al arrancar: si falta el hash de la contraseña, la app no arranca.
 * Las tarifas (precio de bobina, luz, mano de obra, margen...) están en la tabla {@code ajustes}
 * y se cambian desde la pantalla Ajustes.
 */
@Validated
@ConfigurationProperties(prefix = "regalos")
public record RegalosProperties(@Valid @NotNull Admin admin,
                                @Valid @NotNull Manyfold manyfold) {

    public record Admin(@NotBlank String usuario,
                        @NotBlank(message = "Define ADMIN_PASSWORD_HASH con un hash bcrypt (ver README)")
                        String passwordHash) {
    }

    public record Manyfold(@NotBlank String urlBase) {
    }
}
