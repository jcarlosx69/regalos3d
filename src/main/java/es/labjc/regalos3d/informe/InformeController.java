package es.labjc.regalos3d.informe;

import es.labjc.regalos3d.informe.InformeDtos.ImporteAnio;
import es.labjc.regalos3d.informe.InformeDtos.ImporteCliente;
import es.labjc.regalos3d.informe.InformeDtos.ImporteOcasion;
import es.labjc.regalos3d.informe.InformeDtos.ModeloEncargado;
import es.labjc.regalos3d.informe.InformeDtos.Resumen;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Las consultas aceptan {@code ?anio=2026} salvo la de importes por año y la de modelos. */
@Validated
@RestController
@RequestMapping("/api/informes")
public class InformeController {

    private final InformeService service;

    public InformeController(InformeService service) {
        this.service = service;
    }

    @GetMapping("/resumen")
    public Resumen resumen(@RequestParam(required = false) Integer anio) {
        return service.resumen(anio);
    }

    @GetMapping("/por-cliente")
    public List<ImporteCliente> porCliente(@RequestParam(required = false) Integer anio) {
        return service.porCliente(anio);
    }

    @GetMapping("/por-anio")
    public List<ImporteAnio> porAnio() {
        return service.porAnio();
    }

    @GetMapping("/por-ocasion")
    public List<ImporteOcasion> porOcasion(@RequestParam(required = false) Integer anio) {
        return service.porOcasion(anio);
    }

    @GetMapping("/modelos")
    public List<ModeloEncargado> modelos(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limite) {
        return service.modelos(limite);
    }
}
