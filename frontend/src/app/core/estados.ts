import { EstadoEncargo, TipoEncargo } from './modelos-api';

/** Orden del flujo. CANCELADO queda fuera de la secuencia. */
export const FLUJO: EstadoEncargo[] = [
  'PRESUPUESTO', 'ACEPTADO', 'EN_COLA', 'IMPRIMIENDO', 'TERMINADO', 'ENTREGADO', 'COBRADO',
];

export const NOMBRE_ESTADO: Record<EstadoEncargo, string> = {
  PRESUPUESTO: 'Presupuesto',
  ACEPTADO: 'Aceptado',
  EN_COLA: 'En cola',
  IMPRIMIENDO: 'Imprimiendo',
  TERMINADO: 'Terminado',
  ENTREGADO: 'Entregado',
  COBRADO: 'Cobrado',
  CANCELADO: 'Cancelado',
};

export const NOMBRE_TIPO: Record<TipoEncargo, string> = { VENTA: 'Venta', REGALO: 'Regalo' };

/** Estados por los que pasa un encargo según su tipo: los regalos terminan al entregarse. */
export function flujoDe(tipo: TipoEncargo): EstadoEncargo[] {
  return tipo === 'REGALO' ? FLUJO.filter((e) => e !== 'COBRADO') : FLUJO;
}

/** Siguiente estado del flujo, o null si ya es el último (o está cancelado). */
export function siguienteEstado(estado: EstadoEncargo, tipo: TipoEncargo): EstadoEncargo | null {
  const flujo = flujoDe(tipo);
  const i = flujo.indexOf(estado);
  return i >= 0 && i < flujo.length - 1 ? flujo[i + 1] : null;
}

export const EN_CURSO: EstadoEncargo[] = ['ACEPTADO', 'EN_COLA', 'IMPRIMIENDO', 'TERMINADO'];

/** Grupos de la lista de encargos. */
export type GrupoEncargos = 'activos' | 'por-cobrar' | 'finalizados' | 'cancelados' | 'todos';

export function enGrupo(grupo: GrupoEncargos, estado: EstadoEncargo, tipo: TipoEncargo): boolean {
  switch (grupo) {
    case 'activos':
      return estado === 'PRESUPUESTO' || EN_CURSO.includes(estado);
    case 'por-cobrar':
      return tipo === 'VENTA' && estado === 'ENTREGADO';
    case 'finalizados':
      return estado === 'COBRADO' || (tipo === 'REGALO' && estado === 'ENTREGADO');
    case 'cancelados':
      return estado === 'CANCELADO';
    default:
      return true;
  }
}

/** Solo se eliminan presupuestos y cancelados (el backend aplica la misma regla). */
export function sePuedeEliminar(estado: EstadoEncargo): boolean {
  return estado === 'PRESUPUESTO' || estado === 'CANCELADO';
}
