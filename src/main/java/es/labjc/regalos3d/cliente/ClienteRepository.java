package es.labjc.regalos3d.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByTelefono(String telefono);

    boolean existsByTelefono(String telefono);

    boolean existsByTelefonoAndIdNot(String telefono, Long id);

    @Query("""
            select c from Cliente c
            where lower(c.nombre) like lower(concat('%', :texto, '%'))
               or c.telefono like concat('%', :texto, '%')
               or lower(c.email) like lower(concat('%', :texto, '%'))
            order by c.nombre""")
    List<Cliente> buscar(@Param("texto") String texto);

    List<Cliente> findAllByOrderByNombreAsc();
}
