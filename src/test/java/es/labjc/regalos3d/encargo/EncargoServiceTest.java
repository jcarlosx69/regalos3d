package es.labjc.regalos3d.encargo;

import es.labjc.regalos3d.ajustes.AjustesService;
import es.labjc.regalos3d.ajustes.AjustesService.Tarifas;
import es.labjc.regalos3d.cliente.Cliente;
import es.labjc.regalos3d.cliente.ClienteRepository;
import es.labjc.regalos3d.encargo.EncargoDtos.EncargoRequest;
import es.labjc.regalos3d.encargo.EncargoDtos.FilamentoRequest;
import es.labjc.regalos3d.encargo.EncargoDtos.LineaRequest;
import es.labjc.regalos3d.error.ConflictoException;
import es.labjc.regalos3d.error.DatoInvalidoException;
import es.labjc.regalos3d.modelo.Modelo;
import es.labjc.regalos3d.modelo.ModeloRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EncargoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 6);

    @Mock EncargoRepository encargos;
    @Mock EncargoLineaRepository lineas;
    @Mock ClienteRepository clientes;
    @Mock ModeloRepository modelos;
    @Mock AjustesService ajustes;

    EncargoService service;
    Cliente ana;
    Modelo llavero;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(HOY.atStartOfDay(ZoneId.of("Atlantic/Canary")).toInstant(), ZoneId.of("Atlantic/Canary"));
        service = new EncargoService(encargos, lineas, clientes, modelos, ajustes, reloj);

        ana = new Cliente("+34600000001", "Ana");
        ReflectionTestUtils.setField(ana, "id", 1L);
        llavero = new Modelo("ll-1", "Llavero", null);
        ReflectionTestUtils.setField(llavero, "id", 10L);

        when(clientes.findById(1L)).thenReturn(Optional.of(ana));
        when(modelos.findAllById(any())).thenReturn(List.of(llavero));
        when(ajustes.tarifas()).thenReturn(new Tarifas(d("20.00"), d("0.1500"), d("150.0"), d("12.00"), d("0.25"), d("30.00")));
        when(encargos.ultimoNumero(2026)).thenReturn(6);
        when(encargos.saveAndFlush(any(Encargo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lineas.previas(anyLong(), anyCollection(), any())).thenReturn(List.of());
    }

    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    /** 10 llaveros, 8 g de PLA sin precio (se aplica el de los ajustes), 0,5 h impresión, 0,1 h mano de obra. */
    private static EncargoRequest peticion(TipoEncargo tipo, EstadoEncargo estado, boolean manoObra, boolean margen) {
        var linea = new LineaRequest(10L, 10, d("0.50"), d("0.10"), null, null,
                List.of(new FilamentoRequest("PLA", "Negro", d("8"), null)), false, null);
        return new EncargoRequest(1L, tipo, estado, HOY, null, null, manoObra, margen,
                null, null, null, null, null, null, List.of(linea));
    }

    @Test
    void unaVentaNuevaEsUnPresupuestoConNumeroSiguienteYTarifasDeLosAjustes() {
        var r = service.crear(peticion(TipoEncargo.VENTA, null, true, true), false);

        assertThat(r.referencia()).isEqualTo("E-2026-0007");
        assertThat(r.estado()).isEqualTo(EstadoEncargo.PRESUPUESTO);
        assertThat(r.tarifas().margenPct()).isEqualByComparingTo("30.00");
        assertThat(r.lineas().getFirst().filamentos().getFirst().precioKg()).isEqualByComparingTo("20.00");
        assertThat(r.precioFinal()).isEqualByComparingTo("19.50");
        assertThat(r.coste()).isEqualByComparingTo("3.00");
        assertThat(r.beneficio()).isEqualByComparingTo("16.50");
        // en una venta no se avisa de modelos repetidos
        verify(lineas, never()).previas(anyLong(), anyCollection(), any());
    }

    @Test
    void unRegaloNuevoEmpiezaEnColaYSeCalculaAlCoste() {
        var r = service.crear(peticion(TipoEncargo.REGALO, null, false, false), false);

        assertThat(r.estado()).isEqualTo(EstadoEncargo.EN_COLA);
        assertThat(r.precioFinal()).isEqualByComparingTo("3.00");
        assertThat(r.beneficio()).isEqualByComparingTo("0");
    }

    @Test
    void unRegaloRepetidoPideConfirmacionYNoSeGuarda() {
        Encargo previo = new Encargo(2025, 3, ana, TipoEncargo.REGALO, EstadoEncargo.ENTREGADO, LocalDate.of(2025, 12, 24));
        ReflectionTestUtils.setField(previo, "id", 99L);
        EncargoLinea lineaPrevia = new EncargoLinea(llavero, 1);
        previo.anadirLinea(lineaPrevia);
        when(lineas.previas(eq(1L), anyCollection(), isNull())).thenReturn(List.of(lineaPrevia));

        assertThatThrownBy(() -> service.crear(peticion(TipoEncargo.REGALO, null, false, false), false))
                .isInstanceOf(EncargoDuplicadoException.class)
                .hasMessageContaining("Llavero")
                .hasMessageContaining("E-2025-0003")
                .satisfies(e -> assertThat(((EncargoDuplicadoException) e).getPrevios()).hasSize(1));
        verify(encargos, never()).saveAndFlush(any());
    }

    @Test
    void unRegaloRepetidoSeGuardaSiSeConfirma() {
        service.crear(peticion(TipoEncargo.REGALO, null, false, false), true);

        verify(lineas, never()).previas(anyLong(), anyCollection(), any());
        verify(encargos).saveAndFlush(any(Encargo.class));
    }

    @Test
    void unRegaloNoSePuedeCobrar() {
        assertThatThrownBy(() -> service.crear(peticion(TipoEncargo.REGALO, EstadoEncargo.COBRADO, false, false), true))
                .isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void lasFechasDeEntregaYCobroSiguenAlEstado() {
        Encargo e = new Encargo(2026, 1, ana, TipoEncargo.VENTA, EstadoEncargo.IMPRIMIENDO, HOY);
        when(encargos.findConDetalleById(5L)).thenReturn(Optional.of(e));

        var cobrado = service.cambiarEstado(5L, EstadoEncargo.COBRADO);
        assertThat(cobrado.fechaEntrega()).isEqualTo(HOY);
        assertThat(cobrado.fechaCobro()).isEqualTo(HOY);

        var vuelta = service.cambiarEstado(5L, EstadoEncargo.TERMINADO);
        assertThat(vuelta.fechaEntrega()).isNull();
        assertThat(vuelta.fechaCobro()).isNull();
    }

    @Test
    void soloSeEliminanPresupuestosOCancelados() {
        Encargo entregado = new Encargo(2026, 2, ana, TipoEncargo.VENTA, EstadoEncargo.ENTREGADO, HOY);
        when(encargos.findConDetalleById(7L)).thenReturn(Optional.of(entregado));

        assertThatThrownBy(() -> service.borrar(7L)).isInstanceOf(ConflictoException.class)
                .hasMessageContaining("E-2026-0002");
        verify(encargos, never()).delete(any());
    }

    @Test
    void alEditarUnRegaloSinCambiarClienteNiModelosNoSeVuelveAPreguntar() {
        Encargo e = new Encargo(2026, 1, ana, TipoEncargo.REGALO, EstadoEncargo.EN_COLA, HOY);
        ReflectionTestUtils.setField(e, "id", 5L);
        e.setPotenciaW(d("150.0"));
        e.setPrecioKwh(d("0.15"));
        e.setManoObraHora(d("12"));
        e.setAmortizacionHora(d("0.25"));
        e.setMargenPct(d("30"));
        e.anadirLinea(new EncargoLinea(llavero, 1));
        when(encargos.findConDetalleById(5L)).thenReturn(Optional.of(e));

        var r = service.actualizar(5L, peticion(TipoEncargo.REGALO, null, false, false), false);

        assertThat(r.lineas()).hasSize(1);
        assertThat(r.lineas().getFirst().cantidad()).isEqualTo(10);
        verify(lineas, never()).previas(anyLong(), anyCollection(), any());
    }
}
