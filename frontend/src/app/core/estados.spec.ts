import { describe, expect, it } from 'vitest';
import { enGrupo, flujoDe, sePuedeEliminar, siguienteEstado } from './estados';

describe('estados de un encargo', () => {
  it('una venta termina en Cobrado y un regalo en Entregado', () => {
    expect(flujoDe('VENTA').at(-1)).toBe('COBRADO');
    expect(flujoDe('REGALO').at(-1)).toBe('ENTREGADO');
    expect(flujoDe('REGALO')).not.toContain('COBRADO');
  });

  it('propone el siguiente paso del flujo', () => {
    expect(siguienteEstado('PRESUPUESTO', 'VENTA')).toBe('ACEPTADO');
    expect(siguienteEstado('ENTREGADO', 'VENTA')).toBe('COBRADO');
    expect(siguienteEstado('ENTREGADO', 'REGALO')).toBeNull();
    expect(siguienteEstado('COBRADO', 'VENTA')).toBeNull();
    expect(siguienteEstado('CANCELADO', 'VENTA')).toBeNull();
  });

  it('agrupa los encargos para las pestañas de la lista', () => {
    expect(enGrupo('activos', 'PRESUPUESTO', 'VENTA')).toBe(true);
    expect(enGrupo('activos', 'IMPRIMIENDO', 'REGALO')).toBe(true);
    expect(enGrupo('por-cobrar', 'ENTREGADO', 'VENTA')).toBe(true);
    expect(enGrupo('por-cobrar', 'ENTREGADO', 'REGALO')).toBe(false);
    expect(enGrupo('finalizados', 'ENTREGADO', 'REGALO')).toBe(true);
    expect(enGrupo('finalizados', 'COBRADO', 'VENTA')).toBe(true);
    expect(enGrupo('cancelados', 'CANCELADO', 'VENTA')).toBe(true);
    expect(enGrupo('todos', 'CANCELADO', 'REGALO')).toBe(true);
  });

  it('solo se eliminan presupuestos y cancelados', () => {
    expect(sePuedeEliminar('PRESUPUESTO')).toBe(true);
    expect(sePuedeEliminar('CANCELADO')).toBe(true);
    expect(sePuedeEliminar('ENTREGADO')).toBe(false);
  });
});
