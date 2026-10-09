package es.labjc.regalos3d.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Seguridad pensada para el frontal Angular servido desde el mismo origen:
 *
 * <ul>
 *   <li>Login por formulario en {@code POST /api/auth/login} (campos {@code username} y {@code password}),
 *       que crea una sesión con cookie HttpOnly. Responde 204 o 401, sin redirecciones.</li>
 *   <li>CSRF activo: el token viaja en la cookie {@code REGALOS3D-XSRF-TOKEN} y Angular lo devuelve en la
 *       cabecera {@code X-XSRF-TOKEN} automáticamente.</li>
 *   <li>HTTP Basic sigue activo para {@code curl} y los tests (las peticiones que cambian datos necesitan
 *       también el token CSRF).</li>
 *   <li>Sin sesión, la API responde 401 sin cabecera {@code WWW-Authenticate}, para que el navegador no
 *       muestre su ventana de usuario y contraseña.</li>
 *   <li>Todo lo que no es {@code /api/**} (la propia app Angular) es público: no contiene datos.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    /** Debe coincidir con {@code cookieName} en {@code withXsrfConfiguration} de Angular (app.config.ts). */
    static final String CSRF_COOKIE = "REGALOS3D-XSRF-TOKEN";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var sinPopup = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

        // Nombre propio: las cookies no distinguen puertos y en el mismo equipo puede haber otras apps
        var csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookieName(CSRF_COOKIE);

        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/**", "/actuator/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")                       // ruta de Angular; desactiva la página por defecto
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value()))
                        .failureHandler((req, res, ex) -> res.setStatus(HttpStatus.UNAUTHORIZED.value()))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .httpBasic(basic -> basic.authenticationEntryPoint(sinPopup))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(sinPopup))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(RegalosProperties props) {
        var admin = User.withUsername(props.admin().usuario())
                .password(props.admin().passwordHash())
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
