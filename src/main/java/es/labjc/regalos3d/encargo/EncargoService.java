package es.labjc.regalos3d.encargo;

import es.labjc.regalos3d.ajustes.AjustesService;
import es.labjc.regalos3d.ajustes.AjustesService.Tarifas;
import es.labjc.regalos3d.cliente.Cliente;
import es.labjc.regalos3d.cliente.ClienteRepository;
import es.labjc.regalos3d.encargo.EncargoDtos.ArticuloResumen;
import es.labjc.regalos3d.encargo.EncargoDtos.ClienteResumen;
import es.labjc.regalos3d.encargo.EncargoDtos.ComprobacionDuplicado;
import es.labjc.regalos3d.encargo.EncargoDtos.EncargoPrevio;
import es.labjc.regalos3d.encargo.EncargoDtos.EncargoRequest;
import es.labjc.regalos3d.encargo.EncargoDtos.EncargoResponse;
import es.labjc.regalos3d.encargo.EncargoDtos.EncargoResumen;
import es.labjc.regalos3d.encargo.EncargoDtos.FilamentoRequest;
import es.labjc.regalos3d.encargo.EncargoDtos.FilamentoResponse;
import es.labjc.regalos3d.encargo.EncargoDtos.LineaRequest;
import es.labjc.regalos3d.encargo.EncargoDtos.LineaResponse;
import es.labjc.regalos3d.encargo.EncargoDtos.ModeloResumen;
import es.labjc.regalos3d.encargo.EncargoDtos.TarifasEncargo;
import es.labjc.regalos3d.error.ConflictoException;
import es.labjc.regalos3d.error.DatoInvalidoException;
import es.labjc.regalos3d.error.NoEncontradoException;
import es.labjc.regalos3d.modelo.Modelo;
import es.labjc.regalos3d.modelo.ModeloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Encargos de venta y de regalo.
 *
 * <p>Regla de repetido: en un encargo de REGALO, si el cliente ya recibió alguno de los modelos en un encargo
 * anterior no cancelado (venta o regalo), no se guarda salvo que la petición lo confirme. En las ventas no se
 * avisa: que un cliente repita compra es normal.</p>
 */
@Service
@Transactional(readOnly = true)
public class EncargoService {

    private final EncargoRepository encargos;
    private final EncargoLineaRepository lineas;
    private final ClienteRepository clientes;
    private final ModeloRepository modelos;
    private final AjustesService ajustes;
    private final Clock clock;

    public EncargoService(EncargoRepository encargos, EncargoLineaRepository lineas, ClienteRepository clientes,
                          ModeloRepository modelos, AjustesService ajustes, Clock clock) {
        this.encargos = encargos;
        this.lineas = lineas;
        this.clientes = clientes;
        this.modelos = modelos;
        this.ajustes = ajustes;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ consultas

    public List<EncargoResumen> listar(Long clienteId) {
        return encargos.listar(clienteId).stream().map(EncargoService::resumen).toList();
    }

    public EncargoResponse obtener(Long id) {
        return respuesta(cargar(id));
    }

    public ComprobacionDuplicado comprobar(Long clienteId, Collection<Long> modeloIds, Long excluirEncargoId) {
        if (modeloIds == null || modeloIds.isEmpty()) {
            return new ComprobacionDuplicado(false, List.of());
        }
        var previos = previos(clienteId, modeloIds, excluirEncargoId);
        return new ComprobacionDuplicado(!previos.isEmpty(), previos);
    }

    // ------------------------------------------------------------------ altas y cambios

    @Transactional
    public EncargoResponse crear(EncargoRequest req, boolean confirmarDuplicado) {
        Cliente cliente = cliente(req.clienteId());
        Map<Long, Modelo> porId = modelosDe(req.lineas());
        if (req.tipo() == TipoEncargo.REGALO && !confirmarDuplicado) {
            exigirConfirmacion(cliente, porId.keySet(), null);
        }

        Tarifas t = ajustes.tarifas();
        int anio = req.fecha().getYear();
        EstadoEncargo estado = req.estado() != null ? req.estado() : estadoInicial(req.tipo());
        validarEstado(req.tipo(), estado);

        Encargo e = new Encargo(anio, encargos.ultimoNumero(anio) + 1, cliente, req.tipo(), estado, req.fecha());
        e.setPotenciaW(valorO(req.potenciaW(), t.potenciaW()));
        e.setPrecioKwh(valorO(req.precioKwh(), t.precioKwh()));
        e.setManoObraHora(valorO(req.manoObraHora(), t.manoObraHora()));
        e.setAmortizacionHora(valorO(req.amortizacionHora(), t.amortizacionHora()));
        e.setMargenPct(valorO(req.margenPct(), t.margenPct()));
        aplicar(e, req, porId, t.precioKg());
        e.cambiarEstado(estado, hoy());

        e.recalcular();
        return respuesta(encargos.saveAndFlush(e));
    }

    @Transactional
    public EncargoResponse actualizar(Long id, EncargoRequest req, boolean confirmarDuplicado) {
        Encargo e = cargar(id);
        Cliente cliente = cliente(req.clienteId());
        Map<Long, Modelo> porId = modelosDe(req.lineas());

        if (req.tipo() == TipoEncargo.REGALO && !confirmarDuplicado) {
            // Solo se comprueba lo que cambia: modelos nuevos, o todos si cambia el cliente o pasa a ser regalo
            boolean todo = !Objects.equals(e.getCliente().getId(), cliente.getId()) || e.getTipo() != TipoEncargo.REGALO;
            Set<Long> aComprobar = new LinkedHashSet<>(porId.keySet());
            if (!todo) {
                e.getLineas().forEach(l -> aComprobar.remove(l.getModelo().getId()));
            }
            exigirConfirmacion(cliente, aComprobar, e.getId());
        }

        EstadoEncargo estado = req.estado() != null ? req.estado() : e.getEstado();
        validarEstado(req.tipo(), estado);

        e.setCliente(cliente);
        e.setTipo(req.tipo());
        e.setFecha(req.fecha());
        if (req.potenciaW() != null) e.setPotenciaW(req.potenciaW());
        if (req.precioKwh() != null) e.setPrecioKwh(req.precioKwh());
        if (req.manoObraHora() != null) e.setManoObraHora(req.manoObraHora());
        if (req.amortizacionHora() != null) e.setAmortizacionHora(req.amortizacionHora());
        if (req.margenPct() != null) e.setMargenPct(req.margenPct());
        e.vaciarLineas();
        aplicar(e, req, porId, ajustes.tarifas().precioKg());
        if (estado != e.getEstado()) {
            e.cambiarEstado(estado, hoy());
        }

        e.recalcular();
        return respuesta(encargos.saveAndFlush(e));
    }

    @Transactional
    public EncargoResponse cambiarEstado(Long id, EstadoEncargo estado) {
        Encargo e = cargar(id);
        validarEstado(e.getTipo(), estado);
        e.cambiarEstado(estado, hoy());
        return respuesta(encargos.saveAndFlush(e));
    }

    /** Solo se eliminan presupuestos y encargos cancelados; el resto se cancela primero. */
    @Transactional
    public void borrar(Long id) {
        Encargo e = cargar(id);
        if (e.getEstado() != EstadoEncargo.PRESUPUESTO && e.getEstado() != EstadoEncargo.CANCELADO) {
            throw new ConflictoException("Solo se pueden eliminar presupuestos o encargos cancelados. "
                    + "Cancela " + e.getReferencia() + " antes de eliminarlo.");
        }
        encargos.delete(e);
    }

    // ------------------------------------------------------------------ auxiliares

    private void aplicar(Encargo e, EncargoRequest req, Map<Long, Modelo> porId, BigDecimal precioKgDefecto) {
        e.setOcasion(vacioANull(req.ocasion()));
        e.setNotas(vacioANull(req.notas()));
        e.setIncluirManoObra(req.incluirManoObra());
        e.setIncluirMargen(req.incluirMargen());
        e.setPrecioManual(req.precioManual());
        for (LineaRequest lr : req.lineas()) {
            EncargoLinea l = new EncargoLinea(porId.get(lr.modeloId()), lr.cantidad());
            l.setHorasImpresion(lr.horasImpresion());
            l.setHorasManoObra(lr.horasManoObra());
            l.setVariosConcepto(vacioANull(lr.variosConcepto()));
            l.setVariosImporte(lr.variosImporte());
            l.setPrecio(lr.obsequio(), lr.precioManual());
            for (FilamentoRequest f : lr.filamentosONada()) {
                l.anadirFilamento(new LineaFilamento(f.material().trim(), vacioANull(f.color()), f.gramos(),
                        valorO(f.precioKg(), precioKgDefecto)));
            }
            e.anadirLinea(l);
        }
    }

    private void exigirConfirmacion(Cliente cliente, Collection<Long> modeloIds, Long excluir) {
        if (modeloIds.isEmpty()) {
            return;
        }
        var previos = previos(cliente.getId(), modeloIds, excluir);
        if (!previos.isEmpty()) {
            throw new EncargoDuplicadoException(cliente.getNombre(), previos);
        }
    }

    private List<EncargoPrevio> previos(Long clienteId, Collection<Long> modeloIds, Long excluir) {
        return lineas.previas(clienteId, modeloIds, excluir).stream()
                .map(l -> {
                    Encargo e = l.getEncargo();
                    return new EncargoPrevio(e.getId(), e.getReferencia(), e.getFecha(), e.getTipo(), e.getOcasion(),
                            l.getModelo().getId(), l.getModelo().getNombre());
                })
                .toList();
    }

    private static void validarEstado(TipoEncargo tipo, EstadoEncargo estado) {
        if (tipo == TipoEncargo.REGALO && estado == EstadoEncargo.COBRADO) {
            throw new DatoInvalidoException("Un regalo no se cobra: su último estado es Entregado");
        }
    }

    private static EstadoEncargo estadoInicial(TipoEncargo tipo) {
        return tipo == TipoEncargo.REGALO ? EstadoEncargo.EN_COLA : EstadoEncargo.PRESUPUESTO;
    }

    private Map<Long, Modelo> modelosDe(List<LineaRequest> lineasReq) {
        Set<Long> ids = lineasReq.stream().map(LineaRequest::modeloId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Modelo> porId = modelos.findAllById(ids).stream()
                .collect(Collectors.toMap(Modelo::getId, Function.identity()));
        for (Long id : ids) {
            if (!porId.containsKey(id)) {
                throw new NoEncontradoException("Modelo", id);
            }
        }
        return porId;
    }

    private Encargo cargar(Long id) {
        return encargos.findConDetalleById(id).orElseThrow(() -> new NoEncontradoException("Encargo", id));
    }

    private Cliente cliente(Long id) {
        return clientes.findById(id).orElseThrow(() -> new NoEncontradoException("Cliente", id));
    }

    private LocalDate hoy() {
        return LocalDate.now(clock);
    }

    private static BigDecimal valorO(BigDecimal valor, BigDecimal porDefecto) {
        return valor != null ? valor : porDefecto;
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ------------------------------------------------------------------ respuestas

    static EncargoResumen resumen(Encargo e) {
        List<ArticuloResumen> articulos = e.getLineas().stream()
                .map(l -> new ArticuloResumen(l.getModelo().getNombre(), l.getCantidad(), l.isObsequio()))
                .toList();
        int unidades = e.getLineas().stream().mapToInt(EncargoLinea::getCantidad).sum();
        return new EncargoResumen(e.getId(), e.getReferencia(), e.getCliente().getId(), e.getCliente().getNombre(),
                e.getTipo(), e.getEstado(), e.getFecha(), e.getFechaEntrega(), e.getOcasion(), articulos, unidades,
                e.getCoste(), e.getPrecioFinal(), e.getPrecioManual() != null);
    }

    static EncargoResponse respuesta(Encargo e) {
        // Desglose por unidad recalculado con las tarifas del propio encargo (no altera lo guardado)
        var calculo = CalculadoraPrecio.calcular(e.tarifasParaCalculo(),
                e.getLineas().stream().map(EncargoLinea::paraCalculo).toList(), e.getPrecioManual());

        List<LineaResponse> lineasResp = new ArrayList<>();
        for (int i = 0; i < e.getLineas().size(); i++) {
            EncargoLinea l = e.getLineas().get(i);
            var r = calculo.lineas().get(i);
            var m = l.getModelo();
            lineasResp.add(new LineaResponse(l.getId(), l.getPosicion(),
                    new ModeloResumen(m.getId(), m.getNombre(), m.getManyfoldModelId()), l.getCantidad(),
                    l.getHorasImpresion(), l.getHorasManoObra(), l.getVariosConcepto(), l.getVariosImporte(),
                    l.getFilamentos().stream()
                            .map(f -> new FilamentoResponse(f.getId(), f.getMaterial(), f.getColor(), f.getGramos(), f.getPrecioKg()))
                            .toList(),
                    l.isObsequio(), l.getPrecioManual(),
                    r.material(), r.energia(), r.amortizacion(), l.getCosteUnitario(), r.manoObraUnitaria(),
                    r.margenUnitario(), r.precioCalculado(), l.getPrecioUnitario(), l.getImporte()));
        }
        Cliente c = e.getCliente();
        return new EncargoResponse(e.getId(), e.getReferencia(),
                new ClienteResumen(c.getId(), c.getNombre(), c.getTelefono(), c.getEmail(), c.getDireccion()),
                e.getTipo(), e.getEstado(), e.getFecha(), e.getFechaEntrega(), e.getFechaCobro(), e.getOcasion(),
                e.getNotas(), e.isIncluirManoObra(), e.isIncluirMargen(),
                new TarifasEncargo(e.getPotenciaW(), e.getPrecioKwh(), e.getManoObraHora(), e.getAmortizacionHora(),
                        e.getMargenPct()),
                lineasResp, e.getCoste(), e.getManoObra(), e.getMargen(), e.getPrecioCalculado(), e.getPrecioManual(),
                e.getPrecioFinal(), e.getPrecioFinal().subtract(e.getCoste()));
    }
}
