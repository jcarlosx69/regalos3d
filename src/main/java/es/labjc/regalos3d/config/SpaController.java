package es.labjc.regalos3d.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Las rutas de Angular ({@code /personas}, {@code /regalos/nuevo}...) no existen en el servidor: si el usuario
 * recarga la página o abre un enlace directo, se devuelve {@code index.html} y Angular pinta la pantalla.
 *
 * <p>Solo se reenvían rutas sin punto (los ficheros .js, .css... los sirve Spring como estáticos) y que no
 * empiezan por {@code api} ni {@code actuator}, para que un endpoint inexistente siga dando 404.</p>
 */
@Controller
public class SpaController {

    private static final String SEGMENTO = "{s:^(?!api$|actuator$)[^.]+}";

    @GetMapping({
            "/" + SEGMENTO,
            "/" + SEGMENTO + "/{s2:[^.]+}",
            "/" + SEGMENTO + "/{s2:[^.]+}/{s3:[^.]+}"
    })
    public String index() {
        return "forward:/index.html";
    }
}
