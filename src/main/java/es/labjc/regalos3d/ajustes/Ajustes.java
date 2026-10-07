package es.labjc.regalos3d.ajustes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Tarifas por defecto del taller y ajustes de los documentos. Tabla de una sola fila (id = 1). */
@Entity
@Table(name = "ajustes")
public class Ajustes {

    public static final int ID = 1;

    @Id
    private Integer id;

    @Column(name = "precio_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal precioKg;

    @Column(name = "precio_kwh", nullable = false, precision = 6, scale = 4)
    private BigDecimal precioKwh;

    @Column(name = "potencia_w", nullable = false, precision = 6, scale = 1)
    private BigDecimal potenciaW;

    @Column(name = "mano_obra_hora", nullable = false, precision = 6, scale = 2)
    private BigDecimal manoObraHora;

    @Column(name = "amortizacion_hora", nullable = false, precision = 6, scale = 2)
    private BigDecimal amortizacionHora;

    @Column(name = "margen_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal margenPct;

    @Column(name = "validez_presupuesto_dias", nullable = false)
    private Integer validezPresupuestoDias;

    @Column(name = "texto_pie", length = 500)
    private String textoPie;

    protected Ajustes() {
    }

    public Integer getId() { return id; }
    public BigDecimal getPrecioKg() { return precioKg; }
    public BigDecimal getPrecioKwh() { return precioKwh; }
    public BigDecimal getPotenciaW() { return potenciaW; }
    public BigDecimal getManoObraHora() { return manoObraHora; }
    public BigDecimal getAmortizacionHora() { return amortizacionHora; }
    public BigDecimal getMargenPct() { return margenPct; }
    public Integer getValidezPresupuestoDias() { return validezPresupuestoDias; }
    public String getTextoPie() { return textoPie; }

    public void setPrecioKg(BigDecimal precioKg) { this.precioKg = precioKg; }
    public void setPrecioKwh(BigDecimal precioKwh) { this.precioKwh = precioKwh; }
    public void setPotenciaW(BigDecimal potenciaW) { this.potenciaW = potenciaW; }
    public void setManoObraHora(BigDecimal manoObraHora) { this.manoObraHora = manoObraHora; }
    public void setAmortizacionHora(BigDecimal amortizacionHora) { this.amortizacionHora = amortizacionHora; }
    public void setMargenPct(BigDecimal margenPct) { this.margenPct = margenPct; }
    public void setValidezPresupuestoDias(Integer dias) { this.validezPresupuestoDias = dias; }
    public void setTextoPie(String textoPie) { this.textoPie = textoPie; }
}
