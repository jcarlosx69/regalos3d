package es.labjc.regalos3d.encargo;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de un encargo: PRESUPUESTO → ACEPTADO → EN_COLA → IMPRIMIENDO → TERMINADO → ENTREGADO → COBRADO.
 * CANCELADO se puede alcanzar desde cualquier estado. Los regalos terminan en ENTREGADO.
 */
public enum EstadoEncargo {
    PRESUPUESTO,
    ACEPTADO,
    EN_COLA,
    IMPRIMIENDO,
    TERMINADO,
    ENTREGADO,
    COBRADO,
    CANCELADO;

    /** Encargos ya entregados: son los que cuentan como facturados o regalados en los informes. */
    public static final Set<EstadoEncargo> REALIZADOS = EnumSet.of(ENTREGADO, COBRADO);

    /** Aceptados y aún sin entregar. */
    public static final Set<EstadoEncargo> EN_CURSO = EnumSet.of(ACEPTADO, EN_COLA, IMPRIMIENDO, TERMINADO);

    public boolean llegoA(EstadoEncargo otro) {
        return this != CANCELADO && otro != CANCELADO && ordinal() >= otro.ordinal();
    }
}
