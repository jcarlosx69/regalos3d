package es.labjc.regalos3d.modelo;

import es.labjc.regalos3d.error.ConflictoException;
import es.labjc.regalos3d.error.NoEncontradoException;
import es.labjc.regalos3d.modelo.ModeloDtos.ModeloRequest;
import es.labjc.regalos3d.encargo.EncargoLineaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ModeloService {

    private final ModeloRepository modelos;
    private final EncargoLineaRepository lineas;

    public ModeloService(ModeloRepository modelos, EncargoLineaRepository lineas) {
        this.modelos = modelos;
        this.lineas = lineas;
    }

    public List<Modelo> listar(String texto) {
        if (texto == null || texto.isBlank()) {
            return modelos.findAllByOrderByNombreAsc();
        }
        return modelos.findByNombreContainingIgnoreCaseOrderByNombreAsc(texto.trim());
    }

    public Modelo obtener(Long id) {
        return modelos.findById(id).orElseThrow(() -> new NoEncontradoException("Modelo", id));
    }

    public Modelo porManyfoldId(String manyfoldId) {
        return modelos.findByManyfoldModelId(manyfoldId.trim())
                .orElseThrow(() -> new NoEncontradoException("Modelo de Manyfold", manyfoldId));
    }

    @Transactional
    public Modelo crear(ModeloRequest req) {
        String manyfoldId = req.manyfoldModelId().trim();
        if (modelos.existsByManyfoldModelId(manyfoldId)) {
            throw new ConflictoException("El modelo de Manyfold " + manyfoldId + " ya está registrado");
        }
        return modelos.save(new Modelo(manyfoldId, req.nombre().trim(), req.gramosEstimados()));
    }

    @Transactional
    public Modelo actualizar(Long id, ModeloRequest req) {
        Modelo m = obtener(id);
        String manyfoldId = req.manyfoldModelId().trim();
        if (modelos.existsByManyfoldModelIdAndIdNot(manyfoldId, id)) {
            throw new ConflictoException("El modelo de Manyfold " + manyfoldId + " ya está registrado");
        }
        m.setManyfoldModelId(manyfoldId);
        m.setNombre(req.nombre().trim());
        m.setGramosEstimados(req.gramosEstimados());
        return m;
    }

    @Transactional
    public void borrar(Long id) {
        Modelo m = obtener(id);
        if (lineas.existsByModeloId(id)) {
            throw new ConflictoException("No se puede eliminar: el modelo " + m.getNombre() + " figura en encargos");
        }
        modelos.delete(m);
    }
}
