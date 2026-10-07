package es.labjc.regalos3d.encargo;

import es.labjc.regalos3d.encargo.CalculadoraPrecio.Bobina;
import es.labjc.regalos3d.encargo.CalculadoraPrecio.Linea;
import es.labjc.regalos3d.encargo.CalculadoraPrecio.Tarifas;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraPrecioTest {

    /** Tarifas por defecto de la migración V4. */
    private static Tarifas tarifas(boolean manoObra, boolean margen) {
        return new Tarifas(d("150.0"), d("0.1500"), d("12.00"), d("0.25"), d("30.00"), manoObra, margen);
    }

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    private static Linea llavero(int cantidad) {
        return new Linea(cantidad, d("0.50"), d("0.10"), null, List.of(new Bobina(d("8.00"), d("20.00"))));
    }

    private static Linea soporte(int cantidad) {
        return new Linea(cantidad, d("3.00"), d("0.25"), null, List.of(new Bobina(d("45.00"), d("24.00"))));
    }

    @Test
    void desgloseDeUnaLineaPorUnidad() {
        var r = CalculadoraPrecio.calcularLinea(tarifas(true, true), llavero(10));

        assertThat(r.material()).isEqualByComparingTo("0.16");      // 8 g × 20 €/kg
        assertThat(r.energia()).isEqualByComparingTo("0.01");       // 0,5 h × 150 W × 0,15 €/kWh = 0,01125
        assertThat(r.amortizacion()).isEqualByComparingTo("0.13");  // 0,5 h × 0,25 €/h = 0,125 → HALF_UP
        assertThat(r.costeUnitario()).isEqualByComparingTo("0.30");
        assertThat(r.manoObraUnitaria()).isEqualByComparingTo("1.20");
        assertThat(r.margenUnitario()).isEqualByComparingTo("0.45"); // 30 % de 1,50
        assertThat(r.precioUnitario()).isEqualByComparingTo("1.95");
        assertThat(r.importe()).isEqualByComparingTo("19.50");
    }

    @Test
    void elEncargoSumaSusLineas() {
        var r = CalculadoraPrecio.calcular(tarifas(true, true), List.of(llavero(10), soporte(1)), null);

        assertThat(r.coste()).isEqualByComparingTo("4.90");
        assertThat(r.manoObra()).isEqualByComparingTo("15.00");
        assertThat(r.margen()).isEqualByComparingTo("5.97");
        assertThat(r.precioCalculado()).isEqualByComparingTo("25.87");
        assertThat(r.precioFinal()).isEqualByComparingTo("25.87");
        assertThat(r.beneficio()).isEqualByComparingTo("20.97");
    }

    @Test
    void sinManoDeObraNiMargenElPrecioEsElCoste() {
        var r = CalculadoraPrecio.calcular(tarifas(false, false), List.of(soporte(2)), null);

        assertThat(r.manoObra()).isEqualByComparingTo("0");
        assertThat(r.margen()).isEqualByComparingTo("0");
        assertThat(r.precioCalculado()).isEqualByComparingTo("3.80");
        assertThat(r.precioCalculado()).isEqualByComparingTo(r.coste());
    }

    @Test
    void soloMargenSinManoDeObra() {
        var r = CalculadoraPrecio.calcular(tarifas(false, true), List.of(soporte(1)), null);

        assertThat(r.margen()).isEqualByComparingTo("0.57");          // 30 % de 1,90
        assertThat(r.precioCalculado()).isEqualByComparingTo("2.47");
    }

    @Test
    void elPrecioManualMandaSobreElCalculado() {
        var r = CalculadoraPrecio.calcular(tarifas(true, true), List.of(llavero(20)), d("35"));

        assertThat(r.precioCalculado()).isEqualByComparingTo("39.00");
        assertThat(r.precioFinal()).isEqualByComparingTo("35.00");
        assertThat(r.beneficio()).isEqualByComparingTo("29.00");
    }

    @Test
    void elObsequioNoSeCobraPeroSuCosteCuenta() {
        var promo = new Linea(1, d("3.00"), d("0.25"), null, List.of(new Bobina(d("45.00"), d("24.00"))), true, null);
        var r = CalculadoraPrecio.calcular(tarifas(true, true), List.of(llavero(10), promo), null);

        assertThat(r.lineas().get(1).precioCalculado()).isEqualByComparingTo("6.37");
        assertThat(r.lineas().get(1).precioUnitario()).isEqualByComparingTo("0");
        assertThat(r.lineas().get(1).importe()).isEqualByComparingTo("0");
        assertThat(r.coste()).isEqualByComparingTo("4.90");             // 3,00 de llaveros + 1,90 del obsequio
        assertThat(r.precioCalculado()).isEqualByComparingTo("19.50");
        assertThat(r.beneficio()).isEqualByComparingTo("14.60");
        assertThat(r.ajusteArticulos()).isEqualByComparingTo("-6.37");  // lo que se deja de cobrar
    }

    @Test
    void precioManualDeUnArticulo() {
        var linea = new Linea(10, d("0.50"), d("0.10"), null, List.of(new Bobina(d("8.00"), d("20.00"))), false, d("2.5"));
        var r = CalculadoraPrecio.calcular(tarifas(true, true), List.of(linea), null);

        assertThat(r.lineas().getFirst().precioCalculado()).isEqualByComparingTo("1.95");
        assertThat(r.lineas().getFirst().precioUnitario()).isEqualByComparingTo("2.50");
        assertThat(r.precioCalculado()).isEqualByComparingTo("25.00");
        assertThat(r.ajusteArticulos()).isEqualByComparingTo("5.50");   // 10 × (2,50 − 1,95)
        assertThat(r.precioFinal()).isEqualByComparingTo("25.00");
    }

    @Test
    void unObsequioIgnoraSuPrecioManual() {
        var linea = new Linea(2, d("0.50"), null, null, List.of(new Bobina(d("8.00"), d("20.00"))), true, d("4"));
        assertThat(CalculadoraPrecio.calcularLinea(tarifas(true, true), linea).importe()).isEqualByComparingTo("0");
    }

    @Test
    void variosYVariasBobinasPorUnidad() {
        var linea = new Linea(2, d("1.00"), null, d("3.50"),
                List.of(new Bobina(d("30"), d("20")), new Bobina(d("5"), d("22"))));
        var r = CalculadoraPrecio.calcularLinea(tarifas(false, false), linea);

        assertThat(r.material()).isEqualByComparingTo("0.71");        // 0,60 + 0,11
        assertThat(r.varios()).isEqualByComparingTo("3.50");
        assertThat(r.costeUnitario()).isEqualByComparingTo("4.48");   // 0,71 + 0,02 + 0,25 + 3,50
        assertThat(r.importe()).isEqualByComparingTo("8.96");
    }

    @Test
    void lineaSinHorasNiBobinasNoFalla() {
        var r = CalculadoraPrecio.calcular(tarifas(true, true), List.of(new Linea(1, null, null, null, List.of())), null);
        assertThat(r.precioFinal()).isEqualByComparingTo("0");
    }
}
