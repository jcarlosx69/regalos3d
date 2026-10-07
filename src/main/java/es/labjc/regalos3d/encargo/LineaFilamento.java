package es.labjc.regalos3d.encargo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Una bobina usada en una línea. Gramos por unidad. */
@Entity
@Table(name = "linea_filamento")
public class LineaFilamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linea_id", nullable = false)
    private EncargoLinea linea;

    @Column(nullable = false, length = 20)
    private String material;

    @Column(length = 40)
    private String color;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal gramos;

    @Column(name = "precio_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal precioKg;

    protected LineaFilamento() {
    }

    public LineaFilamento(String material, String color, BigDecimal gramos, BigDecimal precioKg) {
        this.material = material;
        this.color = color;
        this.gramos = gramos;
        this.precioKg = precioKg;
    }

    public Long getId() { return id; }
    public EncargoLinea getLinea() { return linea; }
    public String getMaterial() { return material; }
    public String getColor() { return color; }
    public BigDecimal getGramos() { return gramos; }
    public BigDecimal getPrecioKg() { return precioKg; }

    void setLinea(EncargoLinea linea) { this.linea = linea; }
}
