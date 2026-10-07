import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { mensajeError } from './avisos';
import { EurosPipe, TelefonoPipe, fechaCorta } from './formato';

describe('TelefonoPipe', () => {
  const pipe = new TelefonoPipe();

  it('agrupa un móvil normalizado', () => {
    expect(pipe.transform('+34611223344')).toBe('+34 611 22 33 44');
  });

  it('deja tal cual lo que no reconoce', () => {
    expect(pipe.transform('12345')).toBe('12345');
    expect(pipe.transform(null)).toBe('');
  });
});

describe('EurosPipe', () => {
  const pipe = new EurosPipe();

  it('formatea en euros con dos decimales y coma', () => {
    expect(pipe.transform(0.77).replace(/\s/g, ' ')).toBe('0,77 €');
    expect(pipe.transform(1234.5).replace(/\s/g, ' ')).toMatch(/^1\.?234,50 €$/);
  });

  it('muestra una raya si no hay importe', () => {
    expect(pipe.transform(null)).toBe('—');
  });
});

describe('fechaCorta', () => {
  it('convierte yyyy-MM-dd a dd/MM/yyyy sin tocar la zona horaria', () => {
    expect(fechaCorta('2026-03-12')).toBe('12/03/2026');
    expect(fechaCorta('2026-01-01')).toBe('01/01/2026');
  });
});

describe('mensajeError', () => {
  it('usa el detail del ProblemDetail del backend', () => {
    const e = new HttpErrorResponse({ status: 409, error: { status: 409, detail: 'Ya hay una persona con el teléfono +34611223344' } });
    expect(mensajeError(e)).toBe('Ya hay una persona con el teléfono +34611223344');
  });

  it('prioriza el primer campo con error de validación', () => {
    const e = new HttpErrorResponse({ status: 400, error: { status: 400, detail: 'Revisa los campos', campos: { fecha: 'no debe ser nulo' } } });
    expect(mensajeError(e)).toBe('fecha: no debe ser nulo');
  });

  it('explica la falta de conexión', () => {
    expect(mensajeError(new HttpErrorResponse({ status: 0 }))).toContain('No hay conexión');
  });
});
