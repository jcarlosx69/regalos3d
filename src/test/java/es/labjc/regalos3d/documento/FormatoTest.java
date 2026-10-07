package es.labjc.regalos3d.documento;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FormatoTest {

    @Test
    void eurosConMilesYSignoMenos() {
        assertThat(Formato.euros(new BigDecimal("1234.5"))).isEqualTo("1.234,50 €");
        assertThat(Formato.euros(new BigDecimal("0"))).isEqualTo("0,00 €");
        assertThat(Formato.euros(new BigDecimal("-3.40"))).isEqualTo("−3,40 €");
        assertThat(Formato.euros(null)).isEqualTo("0,00 €");
    }

    @Test
    void fechaCorta() {
        assertThat(Formato.fecha(LocalDate.of(2026, 3, 9))).isEqualTo("09/03/2026");
        assertThat(Formato.fecha(null)).isNull();
    }

    @Test
    void movilEspanolEnGruposYElRestoTalCual() {
        assertThat(Formato.telefono("+34611223344")).isEqualTo("611 22 33 44");
        assertThat(Formato.telefono("+441234567890")).isEqualTo("+441234567890");
        assertThat(Formato.telefono(" ")).isNull();
    }

    @Test
    void direccionEnLineasSinVacias() {
        assertThat(Formato.lineas("C/ Inventada 12\r\n\n  35001 Las Palmas ")).containsExactly("C/ Inventada 12", "35001 Las Palmas");
        assertThat(Formato.lineas(null)).isEmpty();
    }
}
