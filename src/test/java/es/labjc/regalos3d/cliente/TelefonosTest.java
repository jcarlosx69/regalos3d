package es.labjc.regalos3d.cliente;

import es.labjc.regalos3d.error.DatoInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelefonosTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "600123456", "600 12 34 56", "600-123-456", "600.123.456",
            "+34600123456", "+34 600 123 456", "0034600123456", "34600123456", " (600) 123 456 "
    })
    void normalizaFormatosHabituales(String entrada) {
        assertThat(Telefonos.normalizar(entrada)).isEqualTo("+34600123456");
    }

    @Test
    void aceptaMovilesQueEmpiezanPor7() {
        assertThat(Telefonos.normalizar("711 22 33 44")).isEqualTo("+34711223344");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "912345678",      // fijo
            "60012345",       // 8 dígitos
            "6001234567",     // 10 dígitos
            "+33612345678",   // Francia
            "60012345a",
            "   "
    })
    void rechazaLoQueNoEsMovilEspanol(String entrada) {
        assertThatThrownBy(() -> Telefonos.normalizar(entrada)).isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void rechazaNulo() {
        assertThatThrownBy(() -> Telefonos.normalizar(null)).isInstanceOf(DatoInvalidoException.class);
    }
}
