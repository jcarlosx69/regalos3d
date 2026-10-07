package es.labjc.regalos3d.documento;

import es.labjc.regalos3d.documento.DocumentoService.Pdf;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Documentos de un encargo en PDF. Se abren en el navegador ({@code inline}); desde ahí se imprimen o se
 * guardan con el nombre "Presupuesto E-2026-0006.pdf". No se cachean: reflejan siempre el encargo actual.
 */
@RestController
@RequestMapping("/api/encargos/{id}")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @GetMapping("/presupuesto.pdf")
    public ResponseEntity<byte[]> presupuesto(@PathVariable long id) {
        return respuesta(service.presupuesto(id));
    }

    @GetMapping("/albaran.pdf")
    public ResponseEntity<byte[]> albaran(@PathVariable long id) {
        return respuesta(service.albaran(id));
    }

    @GetMapping("/etiqueta.pdf")
    public ResponseEntity<byte[]> etiqueta(@PathVariable long id) {
        return respuesta(service.etiqueta(id));
    }

    private static ResponseEntity<byte[]> respuesta(Pdf pdf) {
        var disposicion = ContentDisposition.inline().filename(pdf.nombre(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .cacheControl(CacheControl.noStore())
                .body(pdf.contenido());
    }
}
