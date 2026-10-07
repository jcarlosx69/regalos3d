package es.labjc.regalos3d.error;

import es.labjc.regalos3d.encargo.EncargoDuplicadoException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/** Respuestas de error en formato RFC 9457 (ProblemDetail), con mensajes en castellano. */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(NoEncontradoException.class)
    ProblemDetail noEncontrado(NoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", e.getMessage());
    }

    @ExceptionHandler(DatoInvalidoException.class)
    ProblemDetail datoInvalido(DatoInvalidoException e) {
        return problema(HttpStatus.BAD_REQUEST, "Dato no válido", e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException e) {
        return problema(HttpStatus.CONFLICT, "Conflicto", e.getMessage());
    }

    /** 409 con los encargos previos, para que el frontal muestre qué modelos ya recibió el cliente. */
    @ExceptionHandler(EncargoDuplicadoException.class)
    ProblemDetail duplicado(EncargoDuplicadoException e) {
        ProblemDetail pd = problema(HttpStatus.CONFLICT, "Regalo repetido", e.getMessage());
        pd.setProperty("codigo", "REGALO_DUPLICADO");
        pd.setProperty("previos", e.getPrevios());
        return pd;
    }

    /** Red de seguridad por si dos peticiones simultáneas se saltan la comprobación previa. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto",
                "La operación choca con datos existentes (valor repetido o registro en uso)");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail restriccion(ConstraintViolationException e) {
        return problema(HttpStatus.BAD_REQUEST, "Dato no válido", e.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> campos.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Datos no válidos", "Revisa los campos indicados");
        pd.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(pd);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setTitle(titulo);
        return pd;
    }
}
