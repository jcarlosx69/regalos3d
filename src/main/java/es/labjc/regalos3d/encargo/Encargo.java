package es.labjc.regalos3d.encargo;

import es.labjc.regalos3d.cliente.Cliente;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Encargo de una venta o de un regalo. Las tarifas se copian al crearlo, así que cambiar los ajustes no
 * altera los encargos ya registrados. Los importes se recalculan y guardan cada vez que se modifica.
 */
@Entity
@Table(name = "encargo")
public class Encargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Convert(converter = Conversores.Tipo.class)
    @Column(nullable = false, length = 10)
    private TipoEncargo tipo;

    @Convert(converter = Conversores.Estado.class)
    @Column(nullable = false, length = 12)
    private EstadoEncargo estado;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @Column(name = "fecha_cobro")
    private LocalDate fechaCobro;

    @Column(length = 100)
    private String ocasion;

    @Column(length = 1000)
    private String notas;

    @Column(name = "incluir_mano_obra", nullable = false)
    private boolean incluirManoObra;

    @Column(name = "incluir_margen", nullable = false)
    private boolean incluirMargen;

    @Column(name = "potencia_w", nullable = false, precision = 6, scale = 1)
    private BigDecimal potenciaW;

    @Column(name = "precio_kwh", nullable = false, precision = 6, scale = 4)
    private BigDecimal precioKwh;

    @Column(name = "mano_obra_hora", nullable = false, precision = 6, scale = 2)
    private BigDecimal manoObraHora;

    @Column(name = "amortizacion_hora", nullable = false, precision = 6, scale = 2)
    private BigDecimal amortizacionHora;

    @Column(name = "margen_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal margenPct;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal coste = BigDecimal.ZERO;

    @Column(name = "mano_obra", nullable = false, precision = 10, scale = 2)
    private BigDecimal manoObra = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal margen = BigDecimal.ZERO;

    @Column(name = "precio_calculado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCalculado = BigDecimal.ZERO;

    @Column(name = "precio_manual", precision = 10, scale = 2)
    private BigDecimal precioManual;

    @Column(name = "precio_final", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioFinal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "encargo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion")
    private List<EncargoLinea> lineas = new ArrayList<>();

    protected Encargo() {
    }

    public Encargo(int anio, int numero, Cliente cliente, TipoEncargo tipo, EstadoEncargo estado, LocalDate fecha) {
        this.anio = anio;
        this.numero = numero;
        this.cliente = cliente;
        this.tipo = tipo;
        this.estado = estado;
        this.fecha = fecha;
    }

    /** E-2026-0007 */
    public String getReferencia() {
        return referencia(anio, numero);
    }

    public static String referencia(int anio, int numero) {
        return "E-%d-%04d".formatted(anio, numero);
    }

    public void anadirLinea(EncargoLinea l) {
        l.setEncargo(this);
        l.setPosicion(lineas.size() + 1);
        lineas.add(l);
    }

    public void vaciarLineas() {
        lineas.clear();
    }

    CalculadoraPrecio.Tarifas tarifasParaCalculo() {
        return new CalculadoraPrecio.Tarifas(potenciaW, precioKwh, manoObraHora, amortizacionHora, margenPct,
                incluirManoObra, incluirMargen);
    }

    /** Recalcula y guarda los importes de cada línea y del encargo. */
    CalculadoraPrecio.Resultado recalcular() {
        var resultado = CalculadoraPrecio.calcular(tarifasParaCalculo(),
                lineas.stream().map(EncargoLinea::paraCalculo).toList(), precioManual);
        for (int i = 0; i < lineas.size(); i++) {
            lineas.get(i).aplicarResultado(resultado.lineas().get(i));
        }
        this.coste = resultado.coste();
        this.manoObra = resultado.manoObra();
        this.margen = resultado.margen();
        this.precioCalculado = resultado.precioCalculado();
        this.precioFinal = resultado.precioFinal();
        return resultado;
    }

    /**
     * Cambia de estado y mantiene coherentes las fechas de entrega y cobro: se rellenan con {@code hoy}
     * al llegar a ENTREGADO o COBRADO y se borran si el encargo vuelve atrás.
     */
    void cambiarEstado(EstadoEncargo nuevo, LocalDate hoy) {
        this.estado = nuevo;
        if (nuevo == EstadoEncargo.CANCELADO) {
            return;
        }
        if (nuevo.llegoA(EstadoEncargo.ENTREGADO)) {
            if (fechaEntrega == null) fechaEntrega = hoy;
        } else {
            fechaEntrega = null;
        }
        if (nuevo == EstadoEncargo.COBRADO) {
            if (fechaCobro == null) fechaCobro = hoy;
        } else {
            fechaCobro = null;
        }
    }

    public Long getId() { return id; }
    public Integer getAnio() { return anio; }
    public Integer getNumero() { return numero; }
    public Cliente getCliente() { return cliente; }
    public TipoEncargo getTipo() { return tipo; }
    public EstadoEncargo getEstado() { return estado; }
    public LocalDate getFecha() { return fecha; }
    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public LocalDate getFechaCobro() { return fechaCobro; }
    public String getOcasion() { return ocasion; }
    public String getNotas() { return notas; }
    public boolean isIncluirManoObra() { return incluirManoObra; }
    public boolean isIncluirMargen() { return incluirMargen; }
    public BigDecimal getPotenciaW() { return potenciaW; }
    public BigDecimal getPrecioKwh() { return precioKwh; }
    public BigDecimal getManoObraHora() { return manoObraHora; }
    public BigDecimal getAmortizacionHora() { return amortizacionHora; }
    public BigDecimal getMargenPct() { return margenPct; }
    public BigDecimal getCoste() { return coste; }
    public BigDecimal getManoObra() { return manoObra; }
    public BigDecimal getMargen() { return margen; }
    public BigDecimal getPrecioCalculado() { return precioCalculado; }
    public BigDecimal getPrecioManual() { return precioManual; }
    public BigDecimal getPrecioFinal() { return precioFinal; }
    public List<EncargoLinea> getLineas() { return lineas; }

    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public void setTipo(TipoEncargo tipo) { this.tipo = tipo; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public void setOcasion(String ocasion) { this.ocasion = ocasion; }
    public void setNotas(String notas) { this.notas = notas; }
    public void setIncluirManoObra(boolean incluirManoObra) { this.incluirManoObra = incluirManoObra; }
    public void setIncluirMargen(boolean incluirMargen) { this.incluirMargen = incluirMargen; }
    public void setPotenciaW(BigDecimal potenciaW) { this.potenciaW = potenciaW; }
    public void setPrecioKwh(BigDecimal precioKwh) { this.precioKwh = precioKwh; }
    public void setManoObraHora(BigDecimal manoObraHora) { this.manoObraHora = manoObraHora; }
    public void setAmortizacionHora(BigDecimal amortizacionHora) { this.amortizacionHora = amortizacionHora; }
    public void setMargenPct(BigDecimal margenPct) { this.margenPct = margenPct; }
    public void setPrecioManual(BigDecimal precioManual) { this.precioManual = precioManual; }
}
