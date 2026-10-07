package es.labjc.regalos3d.modelo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModeloRepository extends JpaRepository<Modelo, Long> {

    Optional<Modelo> findByManyfoldModelId(String manyfoldModelId);

    boolean existsByManyfoldModelId(String manyfoldModelId);

    boolean existsByManyfoldModelIdAndIdNot(String manyfoldModelId, Long id);

    List<Modelo> findByNombreContainingIgnoreCaseOrderByNombreAsc(String texto);

    List<Modelo> findAllByOrderByNombreAsc();
}
