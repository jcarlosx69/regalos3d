package es.labjc.regalos3d.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Referencia a un modelo de Manyfold. Los ficheros (STL, 3MF, FCStd, STEP) y las fotos viven en Manyfold;
 * aquí solo se guarda su identificador público y una copia del nombre, para que la app funcione aunque
 * Manyfold no responda.
 */
@Entity
@Table(name = "modelo")
public class Modelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manyfold_model_id", nullable = false, unique = true, length = 64)
    private String manyfoldModelId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "gramos_estimados", precision = 7, scale = 2)
    private BigDecimal gramosEstimados;

    protected Modelo() {
    }

    public Modelo(String manyfoldModelId, String nombre, BigDecimal gramosEstimados) {
        this.manyfoldModelId = manyfoldModelId;
        this.nombre = nombre;
        this.gramosEstimados = gramosEstimados;
    }

    public Long getId() { return id; }
    public String getManyfoldModelId() { return manyfoldModelId; }
    public String getNombre() { return nombre; }
    public BigDecimal getGramosEstimados() { return gramosEstimados; }

    public void setManyfoldModelId(String manyfoldModelId) { this.manyfoldModelId = manyfoldModelId; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setGramosEstimados(BigDecimal gramosEstimados) { this.gramosEstimados = gramosEstimados; }
}
