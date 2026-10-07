package es.labjc.regalos3d.encargo;

import es.labjc.regalos3d.modelo.Modelo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import java.util.ArrayList;
import java.util.List;

/** Un artículo del encargo. Horas, gramos y varios son por unidad. */
@Entity
@Table(name = "encargo_linea")
public class EncargoLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encargo_id", nullable = false)
    private Encargo encargo;

    @Column(nullable = false)
    private Integer posicion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modelo_id", nullable = false)
    private Modelo modelo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "horas_impresion", precision = 6, scale = 2)
    private BigDecimal horasImpresion;

    @Column(name = "horas_mano_obra", precision = 5, scale = 2)
    private BigDecimal horasManoObra;

    @Column(name = "varios_concepto", length = 150)
    private String variosConcepto;

    @Column(name = "varios_importe", precision = 8, scale = 2)
    private BigDecimal variosImporte;

    /** Regalo o promoción dentro del encargo: no se cobra, pero su coste cuenta. */
    @Column(nullable = false)
    private boolean obsequio;

    /** Precio por unidad fijado a mano; vacío: el calculado. */
    @Column(name = "precio_manual", precision = 10, scale = 2)
    private BigDecimal precioManual;

    @Column(name = "coste_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costeUnitario = BigDecimal.ZERO;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importe = BigDecimal.ZERO;

    @OneToMany(mappedBy = "linea", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<LineaFilamento> filamentos = new ArrayList<>();

    protected EncargoLinea() {
    }

    public EncargoLinea(Modelo modelo, int cantidad) {
        this.modelo = modelo;
        this.cantidad = cantidad;
    }

    public void anadirFilamento(LineaFilamento f) {
        f.setLinea(this);
        filamentos.add(f);
    }

    /** Datos que necesita la calculadora. */
    CalculadoraPrecio.Linea paraCalculo() {
        return new CalculadoraPrecio.Linea(cantidad, horasImpresion, horasManoObra, variosImporte,
                filamentos.stream().map(f -> new CalculadoraPrecio.Bobina(f.getGramos(), f.getPrecioKg())).toList(),
                obsequio, precioManual);
    }

    public Long getId() { return id; }
    public Encargo getEncargo() { return encargo; }
    public Integer getPosicion() { return posicion; }
    public Modelo getModelo() { return modelo; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getHorasImpresion() { return horasImpresion; }
    public BigDecimal getHorasManoObra() { return horasManoObra; }
    public String getVariosConcepto() { return variosConcepto; }
    public BigDecimal getVariosImporte() { return variosImporte; }
    public boolean isObsequio() { return obsequio; }
    public BigDecimal getPrecioManual() { return precioManual; }
    public BigDecimal getCosteUnitario() { return costeUnitario; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getImporte() { return importe; }
    public List<LineaFilamento> getFilamentos() { return filamentos; }

    void setEncargo(Encargo encargo) { this.encargo = encargo; }
    void setPosicion(Integer posicion) { this.posicion = posicion; }
    public void setHorasImpresion(BigDecimal horasImpresion) { this.horasImpresion = horasImpresion; }
    public void setHorasManoObra(BigDecimal horasManoObra) { this.horasManoObra = horasManoObra; }
    public void setVariosConcepto(String variosConcepto) { this.variosConcepto = variosConcepto; }
    public void setVariosImporte(BigDecimal variosImporte) { this.variosImporte = variosImporte; }

    /** Un obsequio no guarda precio manual: siempre va a 0. */
    public void setPrecio(boolean obsequio, BigDecimal precioManual) {
        this.obsequio = obsequio;
        this.precioManual = obsequio ? null : precioManual;
    }

    void aplicarResultado(CalculadoraPrecio.ResultadoLinea r) {
        this.costeUnitario = r.costeUnitario();
        this.precioUnitario = r.precioUnitario();
        this.importe = r.importe();
    }
}
