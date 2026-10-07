package es.labjc.regalos3d.documento;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Convierte una plantilla Thymeleaf de {@code classpath:pdf/plantillas} en PDF:
 * Thymeleaf genera el HTML, jsoup lo convierte en un DOM bien formado y openhtmltopdf lo pinta.
 *
 * <p>Se usa Thymeleaf "a secas" (sin el starter de Spring), con su propio motor: así no interfiere con las
 * vistas web, que son la app Angular. Las fuentes y el logo van dentro del jar, sin depender de rutas.</p>
 */
@Component
public class MotorPdf {

    private static final String RAIZ = "pdf/";
    private static final Locale ES = Locale.forLanguageTag("es-ES");

    private record Fuente(String archivo, int peso) {
    }

    private static final List<Fuente> FUENTES = List.of(
            new Fuente("Archivo-Regular.ttf", 400),
            new Fuente("Archivo-SemiBold.ttf", 600),
            new Fuente("Archivo-Bold.ttf", 700));

    private final TemplateEngine plantillas;
    private final String css;
    private final String logo;

    public MotorPdf() {
        var resolver = new ClassLoaderTemplateResolver(MotorPdf.class.getClassLoader());
        resolver.setPrefix(RAIZ + "plantillas/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);
        this.plantillas = new TemplateEngine();
        this.plantillas.setTemplateResolver(resolver);

        this.css = new String(leer(RAIZ + "documento.css"), StandardCharsets.UTF_8);
        this.logo = "data:image/png;base64," + Base64.getEncoder().encodeToString(leer(RAIZ + "logo.png"));
    }

    /** PDF de la plantilla {@code nombre} (presupuesto, albaran o etiqueta) con los datos de {@code d}. */
    public byte[] pdf(String nombre, DocumentoVista d) {
        org.w3c.dom.Document dom = new W3CDom().fromJsoup(Jsoup.parse(html(nombre, d)));

        var builder = new PdfRendererBuilder();
        for (Fuente f : FUENTES) {
            FSSupplier<InputStream> datos = () -> abrir(RAIZ + "fuentes/" + f.archivo());
            builder.useFont(datos, "Archivo", f.peso(), FontStyle.NORMAL, true);
        }
        var salida = new ByteArrayOutputStream();
        builder.withW3cDocument(dom, "/");
        builder.toStream(salida);
        try {
            builder.run();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF '" + nombre + "'", e);
        }
        return salida.toByteArray();
    }

    /** HTML intermedio. Útil para revisar una plantilla sin generar el PDF. */
    String html(String nombre, DocumentoVista d) {
        var ctx = new Context(ES);
        ctx.setVariable("d", d);
        ctx.setVariable("css", css);
        ctx.setVariable("logo", logo);
        return plantillas.process(nombre, ctx);
    }

    private static InputStream abrir(String ruta) {
        InputStream in = MotorPdf.class.getClassLoader().getResourceAsStream(ruta);
        if (in == null) {
            throw new IllegalStateException("Falta el recurso " + ruta);
        }
        return in;
    }

    private static byte[] leer(String ruta) {
        try (InputStream in = abrir(ruta)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + ruta, e);
        }
    }
}
