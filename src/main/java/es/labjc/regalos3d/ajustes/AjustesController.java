package es.labjc.regalos3d.ajustes;

import es.labjc.regalos3d.ajustes.AjustesService.Documentos;
import es.labjc.regalos3d.ajustes.AjustesService.Tarifas;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ajustes")
public class AjustesController {

    private final AjustesService service;

    public AjustesController(AjustesService service) {
        this.service = service;
    }

    @GetMapping
    public Tarifas obtener() {
        return service.tarifas();
    }

    @PutMapping
    public Tarifas guardar(@Valid @RequestBody Tarifas tarifas) {
        return service.guardar(tarifas);
    }

    @GetMapping("/documentos")
    public Documentos documentos() {
        return service.documentos();
    }

    @PutMapping("/documentos")
    public Documentos guardarDocumentos(@Valid @RequestBody Documentos documentos) {
        return service.guardarDocumentos(documentos);
    }
}
