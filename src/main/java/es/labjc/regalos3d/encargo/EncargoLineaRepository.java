package es.labjc.regalos3d.encargo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EncargoLineaRepository extends JpaRepository<EncargoLinea, Long> {

    boolean existsByModeloId(Long modeloId);

    /**
     * Líneas anteriores del mismo cliente con alguno de los modelos dados, en encargos no cancelados.
     * Base del aviso de regalo repetido. {@code excluirEncargoId} evita contar el propio encargo al editarlo.
     */
    default List<EncargoLinea> previas(Long clienteId, Collection<Long> modeloIds, Long excluirEncargoId) {
        return previas(clienteId, modeloIds, excluirEncargoId == null ? -1L : excluirEncargoId,
                EstadoEncargo.CANCELADO);
    }

    @Query("""
            select l from EncargoLinea l
              join fetch l.encargo e
              join fetch l.modelo m
            where e.cliente.id = :clienteId
              and m.id in :modeloIds
              and e.estado <> :cancelado
              and e.id <> :excluirEncargoId
            order by e.fecha desc, e.id desc""")
    List<EncargoLinea> previas(@Param("clienteId") Long clienteId,
                               @Param("modeloIds") Collection<Long> modeloIds,
                               @Param("excluirEncargoId") Long excluirEncargoId,
                               @Param("cancelado") EstadoEncargo cancelado);
}
