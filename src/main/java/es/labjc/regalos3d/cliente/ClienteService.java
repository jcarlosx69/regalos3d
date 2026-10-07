package es.labjc.regalos3d.cliente;

import es.labjc.regalos3d.cliente.ClienteDtos.ClienteRequest;
import es.labjc.regalos3d.encargo.EncargoRepository;
import es.labjc.regalos3d.error.ConflictoException;
import es.labjc.regalos3d.error.NoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clientes;
    private final EncargoRepository encargos;

    public ClienteService(ClienteRepository clientes, EncargoRepository encargos) {
        this.clientes = clientes;
        this.encargos = encargos;
    }

    public List<Cliente> listar(String texto) {
        if (texto == null || texto.isBlank()) {
            return clientes.findAllByOrderByNombreAsc();
        }
        return clientes.buscar(texto.trim());
    }

    public Cliente obtener(Long id) {
        return clientes.findById(id).orElseThrow(() -> new NoEncontradoException("Cliente", id));
    }

    /** Busca por móvil en cualquier formato (600 12 34 56, +34600123456...). */
    public Cliente porTelefono(String telefono) {
        String normalizado = Telefonos.normalizar(telefono);
        return clientes.findByTelefono(normalizado)
                .orElseThrow(() -> new NoEncontradoException("Cliente con teléfono", normalizado));
    }

    @Transactional
    public Cliente crear(ClienteRequest req) {
        String telefono = Telefonos.normalizar(req.telefono());
        if (clientes.existsByTelefono(telefono)) {
            throw new ConflictoException("Ya hay un cliente con el teléfono " + telefono);
        }
        Cliente c = new Cliente(telefono, req.nombre().trim());
        aplicar(c, req);
        return clientes.save(c);
    }

    @Transactional
    public Cliente actualizar(Long id, ClienteRequest req) {
        Cliente c = obtener(id);
        String telefono = Telefonos.normalizar(req.telefono());
        if (clientes.existsByTelefonoAndIdNot(telefono, id)) {
            throw new ConflictoException("Ya hay otro cliente con el teléfono " + telefono);
        }
        c.setTelefono(telefono);
        c.setNombre(req.nombre().trim());
        aplicar(c, req);
        return c;
    }

    @Transactional
    public void borrar(Long id) {
        Cliente c = obtener(id);
        if (encargos.existsByClienteId(id)) {
            throw new ConflictoException("No se puede eliminar: " + c.getNombre() + " tiene encargos registrados");
        }
        clientes.delete(c);
    }

    private static void aplicar(Cliente c, ClienteRequest req) {
        c.setEmail(vacioANull(req.email()));
        c.setDireccion(vacioANull(req.direccion()));
        c.setNotas(vacioANull(req.notas()));
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
