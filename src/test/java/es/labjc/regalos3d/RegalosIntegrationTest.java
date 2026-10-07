package es.labjc.regalos3d;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo contra una MariaDB 10.11 real (Testcontainers): migraciones Flyway (V1-V6),
 * validación del esquema por Hibernate, encargos con líneas, estados, regla de regalo repetido, informes,
 * ajustes, documentos PDF y seguridad. Se salta sola si no hay Docker (o el socket de Podman).
 *
 * <p>Los tests comparten base de datos: cada uno usa clientes y años propios para no interferir.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "regalos.admin.usuario=admin",
        // hash bcrypt de "admin-dev"
        "regalos.admin.password-hash=$2a$10$AAbPBlYqv59r0GL0opoSH.7yFZ5/bNkQgejYYt3nQOuY22PVRRZAe"
})
class RegalosIntegrationTest {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:10.11");

    private static final RequestPostProcessor ADMIN = httpBasic("admin", "admin-dev");
    private static final AtomicInteger MOVIL = new AtomicInteger(600_100_000);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    // ------------------------------------------------------------------ encargos

    @Test
    void ventaConVariasLineasEstadosEInformes() throws Exception {
        long cliente = nuevoCliente("Taller Prueba");
        long llavero = nuevoModelo("llavero-it", "Llavero");
        long soporte = nuevoModelo("soporte-it", "Soporte");

        // Tarifas explícitas (las de V4); importes comprobados en CalculadoraPrecioTest
        String venta = """
                {"clienteId": %d, "tipo": "VENTA", "fecha": "2031-03-01",
                 "incluirManoObra": true, "incluirMargen": true,
                 "potenciaW": 150, "precioKwh": 0.15, "manoObraHora": 12, "amortizacionHora": 0.25, "margenPct": 30,
                 "lineas": [
                   {"modeloId": %d, "cantidad": 10, "horasImpresion": 0.5, "horasManoObra": 0.1,
                    "filamentos": [{"material": "PLA", "color": "Negro", "gramos": 8, "precioKg": 20}]},
                   {"modeloId": %d, "cantidad": 1, "horasImpresion": 3, "horasManoObra": 0.25,
                    "filamentos": [{"material": "PETG", "color": "Gris", "gramos": 45, "precioKg": 24}]}
                 ]}""".formatted(cliente, llavero, soporte);

        JsonNode creado = leer(mvc.perform(escritura(post("/api/encargos")).content(venta))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referencia").value("E-2031-0001"))
                .andExpect(jsonPath("$.estado").value("PRESUPUESTO"))
                .andExpect(jsonPath("$.lineas.length()").value(2))
                .andExpect(jsonPath("$.lineas[0].precioUnitario").value(1.95))
                .andExpect(jsonPath("$.lineas[0].importe").value(19.50))
                .andExpect(jsonPath("$.lineas[1].importe").value(6.37))
                .andExpect(jsonPath("$.coste").value(4.90))
                .andExpect(jsonPath("$.precioFinal").value(25.87))
                .andExpect(jsonPath("$.beneficio").value(20.97)));
        long id = creado.path("id").asLong();

        // el siguiente encargo del mismo año lleva el número siguiente
        mvc.perform(escritura(post("/api/encargos")).content(venta))
                .andExpect(jsonPath("$.referencia").value("E-2031-0002"));

        // un presupuesto no cuenta como facturado
        mvc.perform(get("/api/informes/resumen").with(ADMIN).param("anio", "2031"))
                .andExpect(jsonPath("$.presupuestos").value(2))
                .andExpect(jsonPath("$.facturado").value(0));

        cambiarEstado(id, "ENTREGADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaEntrega").exists())
                .andExpect(jsonPath("$.fechaCobro").doesNotExist());

        mvc.perform(get("/api/informes/resumen").with(ADMIN).param("anio", "2031"))
                .andExpect(jsonPath("$.ventas").value(1))
                .andExpect(jsonPath("$.facturado").value(25.87))
                .andExpect(jsonPath("$.pendienteCobro").value(25.87))
                .andExpect(jsonPath("$.beneficio").value(20.97));

        cambiarEstado(id, "COBRADO").andExpect(jsonPath("$.fechaCobro").exists());
        mvc.perform(get("/api/informes/resumen").with(ADMIN).param("anio", "2031"))
                .andExpect(jsonPath("$.cobrado").value(25.87))
                .andExpect(jsonPath("$.pendienteCobro").value(0));

        mvc.perform(get("/api/informes/modelos").with(ADMIN))
                .andExpect(status().isOk());

        // un encargo cobrado no se elimina; cancelado sí
        mvc.perform(delete("/api/encargos/{id}", id).with(ADMIN).with(csrf())).andExpect(status().isConflict());
        cambiarEstado(id, "CANCELADO").andExpect(status().isOk());
        mvc.perform(delete("/api/encargos/{id}", id).with(ADMIN).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/encargos/{id}", id).with(ADMIN)).andExpect(status().isNotFound());
    }

    @Test
    void elPrecioManualMandaYSeConservaElCalculado() throws Exception {
        long cliente = nuevoCliente("Con descuento");
        long modelo = nuevoModelo("manual-it", "Pieza");
        String cuerpo = """
                {"clienteId": %d, "tipo": "VENTA", "fecha": "2032-01-10", "incluirManoObra": true, "incluirMargen": true,
                 "potenciaW": 150, "precioKwh": 0.15, "manoObraHora": 12, "amortizacionHora": 0.25, "margenPct": 30,
                 "precioManual": 35,
                 "lineas": [{"modeloId": %d, "cantidad": 20, "horasImpresion": 0.5, "horasManoObra": 0.1,
                             "filamentos": [{"material": "PLA", "gramos": 8, "precioKg": 20}]}]}"""
                .formatted(cliente, modelo);

        mvc.perform(escritura(post("/api/encargos")).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precioCalculado").value(39.00))
                .andExpect(jsonPath("$.precioManual").value(35.00))
                .andExpect(jsonPath("$.precioFinal").value(35.00))
                .andExpect(jsonPath("$.beneficio").value(29.00));
    }

    @Test
    void precioManualPorArticuloYObsequioSinCargo() throws Exception {
        long cliente = nuevoCliente("Con obsequio");
        long llavero = nuevoModelo("llavero-promo-it", "Llavero");
        long soporte = nuevoModelo("soporte-promo-it", "Soporte");
        // El soporte va de obsequio: su precio manual se descarta y no se cobra, pero su coste cuenta
        String cuerpo = """
                {"clienteId": %d, "tipo": "VENTA", "fecha": "2038-02-01", "incluirManoObra": true, "incluirMargen": true,
                 "potenciaW": 150, "precioKwh": 0.15, "manoObraHora": 12, "amortizacionHora": 0.25, "margenPct": 30,
                 "lineas": [
                   {"modeloId": %d, "cantidad": 10, "horasImpresion": 0.5, "horasManoObra": 0.1, "precioManual": 2.50,
                    "filamentos": [{"material": "PLA", "gramos": 8, "precioKg": 20}]},
                   {"modeloId": %d, "cantidad": 1, "horasImpresion": 3, "horasManoObra": 0.25,
                    "obsequio": true, "precioManual": 9,
                    "filamentos": [{"material": "PETG", "gramos": 45, "precioKg": 24}]}
                 ]}""".formatted(cliente, llavero, soporte);

        JsonNode creado = leer(mvc.perform(escritura(post("/api/encargos")).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lineas[0].precioCalculado").value(1.95))
                .andExpect(jsonPath("$.lineas[0].precioManual").value(2.50))
                .andExpect(jsonPath("$.lineas[0].importe").value(25.00))
                .andExpect(jsonPath("$.lineas[1].obsequio").value(true))
                .andExpect(jsonPath("$.lineas[1].precioManual").doesNotExist())
                .andExpect(jsonPath("$.lineas[1].precioCalculado").value(6.37))
                .andExpect(jsonPath("$.lineas[1].importe").value(0))
                .andExpect(jsonPath("$.coste").value(4.90))
                .andExpect(jsonPath("$.precioFinal").value(25.00))
                .andExpect(jsonPath("$.beneficio").value(20.10)));

        mvc.perform(get("/api/encargos").with(ADMIN).param("clienteId", String.valueOf(cliente)))
                .andExpect(jsonPath("$[0].referencia").value(creado.path("referencia").asText()))
                .andExpect(jsonPath("$[0].articulos[1].obsequio").value(true));
    }

    @Test
    void regaloRepetidoPideConfirmacionPeroUnaVentaNo() throws Exception {
        long cliente = nuevoCliente("Destinatario");
        long conejo = nuevoModelo("conejo-it", "Conejo");

        String regalo = """
                {"clienteId": %d, "tipo": "REGALO", "fecha": "2033-03-12", "ocasion": "Cumpleaños",
                 "incluirManoObra": false, "incluirMargen": false,
                 "lineas": [{"modeloId": %d, "cantidad": 1, "horasImpresion": 2,
                             "filamentos": [{"material": "PLA", "gramos": 30}]}]}""".formatted(cliente, conejo);

        mvc.perform(escritura(post("/api/encargos")).content(regalo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_COLA"))
                .andExpect(jsonPath("$.lineas[0].filamentos[0].precioKg").value(20.0)); // precio de bobina por defecto

        mvc.perform(get("/api/encargos/comprobar").with(ADMIN)
                        .param("clienteId", String.valueOf(cliente)).param("modeloIds", String.valueOf(conejo)))
                .andExpect(jsonPath("$.duplicado").value(true))
                .andExpect(jsonPath("$.previos[0].referencia").value("E-2033-0001"));

        mvc.perform(escritura(post("/api/encargos")).content(regalo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGALO_DUPLICADO"))
                .andExpect(jsonPath("$.previos[0].modelo").value("Conejo"));

        mvc.perform(escritura(post("/api/encargos")).param("confirmarDuplicado", "true").content(regalo))
                .andExpect(status().isCreated());

        // como venta, el mismo modelo al mismo cliente no avisa
        mvc.perform(escritura(post("/api/encargos")).content(regalo.replace("\"REGALO\"", "\"VENTA\"")))
                .andExpect(status().isCreated());
    }

    @Test
    void unRegaloNoSeCobra() throws Exception {
        long cliente = nuevoCliente("Sin cobro");
        long modelo = nuevoModelo("sincobro-it", "Figura");
        String regalo = """
                {"clienteId": %d, "tipo": "REGALO", "fecha": "2034-01-01", "incluirManoObra": false, "incluirMargen": false,
                 "lineas": [{"modeloId": %d, "cantidad": 1}]}""".formatted(cliente, modelo);
        long id = leer(mvc.perform(escritura(post("/api/encargos")).content(regalo)).andExpect(status().isCreated()))
                .path("id").asLong();

        cambiarEstado(id, "COBRADO").andExpect(status().isBadRequest());
        cambiarEstado(id, "ENTREGADO").andExpect(status().isOk());
    }

    @Test
    void unEncargoNecesitaAlMenosUnArticulo() throws Exception {
        long cliente = nuevoCliente("Vacío");
        mvc.perform(escritura(post("/api/encargos")).content("""
                        {"clienteId": %d, "tipo": "VENTA", "fecha": "2035-01-01", "lineas": []}""".formatted(cliente)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.lineas").exists());
    }

    // ------------------------------------------------------------------ ajustes

    @Test
    void lasTarifasSeCambianDesdeAjustesYLasUsanLosEncargosNuevos() throws Exception {
        String original = mvc.perform(get("/api/ajustes").with(ADMIN))
                .andExpect(jsonPath("$.precioKg").value(20.0))
                .andExpect(jsonPath("$.manoObraHora").value(12.0))
                .andExpect(jsonPath("$.amortizacionHora").value(0.25))
                .andExpect(jsonPath("$.margenPct").value(30.0))
                .andReturn().getResponse().getContentAsString();
        try {
            mvc.perform(escritura(put("/api/ajustes")).content("""
                            {"precioKg": 25, "precioKwh": 0.2, "potenciaW": 120, "manoObraHora": 15,
                             "amortizacionHora": 0.3, "margenPct": 40}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.margenPct").value(40.0));

            long cliente = nuevoCliente("Tarifa nueva");
            long modelo = nuevoModelo("tarifa-it", "Taza");
            mvc.perform(escritura(post("/api/encargos")).content("""
                            {"clienteId": %d, "tipo": "VENTA", "fecha": "2036-01-01", "incluirManoObra": true, "incluirMargen": true,
                             "lineas": [{"modeloId": %d, "cantidad": 1, "filamentos": [{"material": "PLA", "gramos": 100}]}]}"""
                            .formatted(cliente, modelo)))
                    .andExpect(jsonPath("$.tarifas.margenPct").value(40.0))
                    .andExpect(jsonPath("$.lineas[0].filamentos[0].precioKg").value(25.0))
                    .andExpect(jsonPath("$.precioFinal").value(3.50)); // 2,50 de material + 40 %
        } finally {
            mvc.perform(escritura(put("/api/ajustes")).content(original)).andExpect(status().isOk());
        }
    }

    // ------------------------------------------------------------------ documentos

    @Test
    void documentosPdfDeUnaVentaYDeUnRegalo() throws Exception {
        long cliente = nuevoCliente("Cliente PDF");
        long modelo = nuevoModelo("pdf-it", "Figura");
        String plantilla = """
                {"clienteId": %d, "tipo": "%s", "fecha": "2039-05-02", "incluirManoObra": false, "incluirMargen": false,
                 "lineas": [{"modeloId": %d, "cantidad": 2, "horasImpresion": 1,
                             "filamentos": [{"material": "PLA", "color": "Verde", "gramos": 20, "precioKg": 20}]}]}""";
        long venta = leer(mvc.perform(escritura(post("/api/encargos")).content(plantilla.formatted(cliente, "VENTA", modelo)))
                .andExpect(status().isCreated())).path("id").asLong();
        // mismo modelo que la venta: el regalo se confirma como repetido a propósito
        long regalo = leer(mvc.perform(escritura(post("/api/encargos").param("confirmarDuplicado", "true"))
                        .content(plantilla.formatted(cliente, "REGALO", modelo)))
                .andExpect(status().isCreated())).path("id").asLong();

        for (String doc : new String[] {"presupuesto", "albaran", "etiqueta"}) {
            byte[] pdf = mvc.perform(get("/api/encargos/{id}/" + doc + ".pdf", venta).with(ADMIN))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/pdf"))
                    .andExpect(header().string("Content-Disposition", containsString("inline")))
                    .andExpect(header().string("Content-Disposition", containsString("E-2039-0001")))
                    .andExpect(header().string("Cache-Control", containsString("no-store")))
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        }

        // un regalo tiene albarán y etiqueta, pero no presupuesto
        mvc.perform(get("/api/encargos/{id}/albaran.pdf", regalo).with(ADMIN)).andExpect(status().isOk());
        mvc.perform(get("/api/encargos/{id}/presupuesto.pdf", regalo).with(ADMIN)).andExpect(status().isConflict());

        mvc.perform(get("/api/encargos/{id}/albaran.pdf", 999_999).with(ADMIN)).andExpect(status().isNotFound());
        mvc.perform(get("/api/encargos/{id}/albaran.pdf", venta)).andExpect(status().isUnauthorized());
    }

    @Test
    void ajustesDeLosDocumentos() throws Exception {
        mvc.perform(get("/api/ajustes/documentos").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validezPresupuestoDias").value(30))
                .andExpect(jsonPath("$.textoPie").doesNotExist());
        try {
            mvc.perform(escritura(put("/api/ajustes/documentos")).content("""
                            {"validezPresupuestoDias": 15, "textoPie": "  Gracias por tu confianza.  "}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.validezPresupuestoDias").value(15))
                    .andExpect(jsonPath("$.textoPie").value("Gracias por tu confianza."));

            mvc.perform(escritura(put("/api/ajustes/documentos")).content("""
                            {"validezPresupuestoDias": 0}"""))
                    .andExpect(status().isBadRequest());
        } finally {
            mvc.perform(escritura(put("/api/ajustes/documentos")).content("""
                    {"validezPresupuestoDias": 30, "textoPie": ""}""")).andExpect(status().isOk());
        }
    }

    // ------------------------------------------------------------------ clientes

    @Test
    void clienteConDatosOpcionalesYMovilUnico() throws Exception {
        mvc.perform(escritura(post("/api/clientes")).content("""
                        {"telefono": "622 00 00 01", "nombre": "Uno", "email": "uno@example.com",
                         "direccion": "C/ Falsa 1"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telefono").value("+34622000001"))
                .andExpect(jsonPath("$.email").value("uno@example.com"));

        mvc.perform(escritura(post("/api/clientes")).content("""
                        {"telefono": "+34 622-000-001", "nombre": "Otro"}"""))
                .andExpect(status().isConflict());

        mvc.perform(escritura(post("/api/clientes")).content("""
                        {"telefono": "633 00 00 09", "nombre": "Email malo", "email": "no-es-un-email"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists());

        mvc.perform(escritura(post("/api/clientes")).content("""
                        {"telefono": "912345678", "nombre": "Fijo"}"""))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/clientes/telefono/{t}", "0034622000001").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Uno"));
    }

    @Test
    void noSeEliminaUnClienteConEncargos() throws Exception {
        long cliente = nuevoCliente("Con historial");
        long modelo = nuevoModelo("historial-it", "Placa");
        mvc.perform(escritura(post("/api/encargos")).content("""
                        {"clienteId": %d, "tipo": "VENTA", "fecha": "2037-01-01", "lineas": [{"modeloId": %d, "cantidad": 1}]}"""
                        .formatted(cliente, modelo)))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/clientes/{id}", cliente).with(ADMIN).with(csrf())).andExpect(status().isConflict());
        mvc.perform(delete("/api/modelos/{id}", modelo).with(ADMIN).with(csrf())).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ seguridad

    @Test
    void sinCredencialesNoHayAccesoNiVentanaDelNavegador() throws Exception {
        mvc.perform(get("/api/encargos"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        mvc.perform(get("/api/encargos").with(httpBasic("admin", "mala"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void loginPorFormularioCreaSesionYLogoutLaCierra() throws Exception {
        var login = mvc.perform(formLogin("/api/auth/login").user("admin").password("admin-dev"))
                .andExpect(status().isNoContent())
                .andReturn();
        var sesion = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/api/auth/yo").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("admin"));

        mvc.perform(post("/api/auth/logout").session(sesion).with(csrf())).andExpect(status().isNoContent());
        assertThat(sesion.isInvalid()).isTrue();
    }

    @Test
    void loginConContrasenaMalaDa401() throws Exception {
        mvc.perform(formLogin("/api/auth/login").user("admin").password("mala")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinTokenCsrfNoSePuedenCambiarDatos() throws Exception {
        mvc.perform(post("/api/clientes").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telefono": "633 00 00 01", "nombre": "Sin token"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    // csrf() de spring-security-test cambia el repositorio CSRF del filtro (compartido) por uno de sesión:
    // con un contexto limpio se prueba el CookieCsrfTokenRepository real
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void entregaLaCookieCsrfQueLeeAngular() throws Exception {
        mvc.perform(get("/api/auth/yo"))
                .andExpect(cookie().exists("REGALOS3D-XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("REGALOS3D-XSRF-TOKEN", false));
    }

    @Test
    void lasRutasDeAngularDevuelvenIndexHtml() throws Exception {
        mvc.perform(get("/encargos/nuevo")).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/clientes")).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/api/no-existe").with(ADMIN))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", containsString("json")));
    }

    // ------------------------------------------------------------------ utilidades

    private long nuevoCliente(String nombre) throws Exception {
        String movil = String.valueOf(MOVIL.incrementAndGet()); // 600100001, 600100002...
        return leer(mvc.perform(escritura(post("/api/clientes")).content("""
                        {"telefono": "%s", "nombre": "%s"}""".formatted(movil, nombre)))
                .andExpect(status().isCreated())).path("id").asLong();
    }

    private long nuevoModelo(String manyfoldId, String nombre) throws Exception {
        return leer(mvc.perform(escritura(post("/api/modelos")).content("""
                        {"manyfoldModelId": "%s", "nombre": "%s"}""".formatted(manyfoldId, nombre)))
                .andExpect(status().isCreated())).path("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions cambiarEstado(long id, String estado) throws Exception {
        return mvc.perform(escritura(patch("/api/encargos/{id}/estado", id)).content("""
                {"estado": "%s"}""".formatted(estado)));
    }

    private MockHttpServletRequestBuilder escritura(MockHttpServletRequestBuilder peticion) {
        return peticion.with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON);
    }

    private JsonNode leer(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }
}
