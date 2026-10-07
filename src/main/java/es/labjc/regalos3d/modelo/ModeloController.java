package es.labjc.regalos3d.modelo;

import es.labjc.regalos3d.config.RegalosProperties;
import es.labjc.regalos3d.modelo.ModeloDtos.ModeloRequest;
import es.labjc.regalos3d.modelo.ModeloDtos.ModeloResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/modelos")
public class ModeloController {

    private final ModeloService service;
    private final String manyfoldUrlBase;

    public ModeloController(ModeloService service, RegalosProperties props) {
        this.service = service;
        this.manyfoldUrlBase = props.manyfold().urlBase().replaceAll("/+$", "");
    }

    @GetMapping
    public List<ModeloResponse> listar(@RequestParam(name = "q", required = false) String q) {
        return service.listar(q).stream().map(this::aResponse).toList();
    }

    @GetMapping("/{id}")
    public ModeloResponse obtener(@PathVariable Long id) {
        return aResponse(service.obtener(id));
    }

    @GetMapping("/manyfold/{manyfoldId}")
    public ModeloResponse porManyfoldId(@PathVariable String manyfoldId) {
        return aResponse(service.porManyfoldId(manyfoldId));
    }

    @PostMapping
    public ResponseEntity<ModeloResponse> crear(@Valid @RequestBody ModeloRequest req) {
        Modelo m = service.crear(req);
        var uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(m.getId()).toUri();
        return ResponseEntity.created(uri).body(aResponse(m));
    }

    @PutMapping("/{id}")
    public ModeloResponse actualizar(@PathVariable Long id, @Valid @RequestBody ModeloRequest req) {
        return aResponse(service.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@PathVariable Long id) {
        service.borrar(id);
    }

    private ModeloResponse aResponse(Modelo m) {
        return ModeloResponse.de(m, manyfoldUrlBase);
    }
}
