package es.labjc.regalos3d.encargo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EncargoRepository extends JpaRepository<Encargo, Long> {

    boolean existsByClienteId(Long clienteId);

    @Query("select coalesce(max(e.numero), 0) from Encargo e where e.anio = :anio")
    int ultimoNumero(@Param("anio") int anio);

    /** Ficha completa. Las bobinas de cada línea se cargan aparte (default_batch_fetch_size). */
    @EntityGraph(attributePaths = {"cliente", "lineas", "lineas.modelo"})
    Optional<Encargo> findConDetalleById(Long id);

    @EntityGraph(attributePaths = {"cliente", "lineas", "lineas.modelo"})
    @Query("""
            select distinct e from Encargo e
            where (:clienteId is null or e.cliente.id = :clienteId)
            order by e.fecha desc, e.id desc""")
    List<Encargo> listar(@Param("clienteId") Long clienteId);
}
