package es.labjc.regalos3d.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cliente o destinatario de un regalo. El móvil (normalizado a +34XXXXXXXXX) lo identifica. */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String telefono;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 150)
    private String email;

    @Column(length = 300)
    private String direccion;

    @Column(length = 500)
    private String notas;

    protected Cliente() {
    }

    public Cliente(String telefono, String nombre) {
        this.telefono = telefono;
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getTelefono() { return telefono; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getDireccion() { return direccion; }
    public String getNotas() { return notas; }

    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setEmail(String email) { this.email = email; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public void setNotas(String notas) { this.notas = notas; }
}
