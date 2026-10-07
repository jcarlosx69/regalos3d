package es.labjc.regalos3d.encargo;

import es.labjc.regalos3d.encargo.EncargoDtos.CambioEstado;
import es.labjc.regalos3d.encargo.EncargoDtos.ComprobacionDuplicado;
import es.labjc.regalos3d.encargo.EncargoDtos.EncargoRequest;
import es.labjc.regalos3d.encargo.EncargoDtos.EncargoResponse;
import es.labjc.regalos3d.encargo.EncargoDtos.EncargoResumen;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/encargos")
public class EncargoController {

    private final EncargoService service;

    public EncargoController(EncargoService service) {
        this.service = service;
    }

    /** Todos los encargos, o los de un cliente: {@code ?clienteId=3}. Del más reciente al más antiguo. */
    @GetMapping
    public List<EncargoResumen> listar(@RequestParam(required = false) Long clienteId) {
        return service.listar(clienteId);
    }

    @GetMapping("/{id}")
    public EncargoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** ¿El cliente ya recibió alguno de estos modelos? {@code ?clienteId=1&modeloIds=4,7&excluir=12} */
    @GetMapping("/comprobar")
    public ComprobacionDuplicado comprobar(@RequestParam Long clienteId,
                                           @RequestParam List<Long> modeloIds,
                                           @RequestParam(required = false) Long excluir) {
        return service.comprobar(clienteId, modeloIds, excluir);
    }

    /** 409 REGALO_DUPLICADO si es un regalo repetido; para guardarlo igualmente, {@code ?confirmarDuplicado=true}. */
    @PostMapping
    public ResponseEntity<EncargoResponse> crear(@Valid @RequestBody EncargoRequest req,
                                                 @RequestParam(defaultValue = "false") boolean confirmarDuplicado) {
        EncargoResponse r = service.crear(req, confirmarDuplicado);
        var uri = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(r.id()).toUri();
        return ResponseEntity.created(uri).body(r);
    }

    @PutMapping("/{id}")
    public EncargoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EncargoRequest req,
                                      @RequestParam(defaultValue = "false") boolean confirmarDuplicado) {
        return service.actualizar(id, req, confirmarDuplicado);
    }

    @PatchMapping("/{id}/estado")
    public EncargoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstado cambio) {
        return service.cambiarEstado(id, cambio.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@PathVariable Long id) {
        service.borrar(id);
    }
}
