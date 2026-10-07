package es.labjc.regalos3d.config;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** El login y el logout los gestiona Spring Security (ver {@link SecurityConfig}). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record UsuarioActual(String usuario) {
    }

    /** 200 con el usuario si hay sesión; 401 si no. Angular lo usa al arrancar para saber si mostrar el login. */
    @GetMapping("/yo")
    public UsuarioActual yo(Authentication auth) {
        return new UsuarioActual(auth.getName());
    }
}
