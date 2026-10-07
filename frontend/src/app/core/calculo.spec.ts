import { describe, expect, it } from 'vitest';
import { LineaCalculo, TarifasCalculo, calcularEncargo, calcularLinea, centimos } from './calculo';

// Mismos casos que CalculadoraPrecioTest.java: los dos cálculos deben coincidir
const tarifas = (manoObra: boolean, margen: boolean): TarifasCalculo => ({
  potenciaW: 150, precioKwh: 0.15, manoObraHora: 12, amortizacionHora: 0.25, margenPct: 30,
  incluirManoObra: manoObra, incluirMargen: margen,
});
const llavero = (cantidad: number): LineaCalculo => ({
  cantidad, horasImpresion: 0.5, horasManoObra: 0.1, variosImporte: null, bobinas: [{ gramos: 8, precioKg: 20 }],
});
const soporte = (cantidad: number): LineaCalculo => ({
  cantidad, horasImpresion: 3, horasManoObra: 0.25, variosImporte: null, bobinas: [{ gramos: 45, precioKg: 24 }],
});

describe('cálculo de precios', () => {
  it('redondea a céntimos mitad hacia arriba sin errores de coma flotante', () => {
    expect(centimos(0.125)).toBe(0.13);
    expect(centimos(1.005)).toBe(1.01);
    expect(centimos(0.01125)).toBe(0.01);
  });

  it('desglosa una línea por unidad', () => {
    const r = calcularLinea(tarifas(true, true), llavero(10));
    expect(r).toEqual({
      material: 0.16, energia: 0.01, amortizacion: 0.13, varios: 0, costeUnitario: 0.3,
      manoObraUnitaria: 1.2, margenUnitario: 0.45, precioCalculado: 1.95, precioUnitario: 1.95, importe: 19.5,
    });
  });

  it('suma las líneas del encargo', () => {
    const r = calcularEncargo(tarifas(true, true), [llavero(10), soporte(1)], null);
    expect(r.coste).toBe(4.9);
    expect(r.manoObra).toBe(15);
    expect(r.margen).toBe(5.97);
    expect(r.precioCalculado).toBe(25.87);
    expect(r.beneficio).toBe(20.97);
  });

  it('sin mano de obra ni margen el precio es el coste', () => {
    const r = calcularEncargo(tarifas(false, false), [soporte(2)], null);
    expect(r.precioCalculado).toBe(3.8);
    expect(r.precioCalculado).toBe(r.coste);
  });

  it('el precio manual manda sobre el calculado', () => {
    const r = calcularEncargo(tarifas(true, true), [llavero(20)], 35);
    expect(r.precioCalculado).toBe(39);
    expect(r.precioFinal).toBe(35);
    expect(r.beneficio).toBe(29);
  });

  it('el obsequio no se cobra pero su coste cuenta', () => {
    const r = calcularEncargo(tarifas(true, true), [llavero(10), { ...soporte(1), obsequio: true }], null);
    expect(r.lineas[1].precioCalculado).toBe(6.37);
    expect(r.lineas[1].importe).toBe(0);
    expect(r.coste).toBe(4.9);
    expect(r.precioCalculado).toBe(19.5);
    expect(r.beneficio).toBe(14.6);
    expect(r.ajusteArticulos).toBe(-6.37);
  });

  it('precio manual de un artículo', () => {
    const r = calcularEncargo(tarifas(true, true), [{ ...llavero(10), precioManual: 2.5 }], null);
    expect(r.lineas[0].precioCalculado).toBe(1.95);
    expect(r.lineas[0].precioUnitario).toBe(2.5);
    expect(r.precioCalculado).toBe(25);
    expect(r.ajusteArticulos).toBe(5.5);
  });

  it('un obsequio ignora su precio manual', () => {
    expect(calcularLinea(tarifas(true, true), { ...llavero(2), obsequio: true, precioManual: 4 }).importe).toBe(0);
  });

  it('varios y varias bobinas por unidad', () => {
    const r = calcularLinea(tarifas(false, false), {
      cantidad: 2, horasImpresion: 1, horasManoObra: null, variosImporte: 3.5,
      bobinas: [{ gramos: 30, precioKg: 20 }, { gramos: 5, precioKg: 22 }],
    });
    expect(r.material).toBe(0.71);
    expect(r.costeUnitario).toBe(4.48);
    expect(r.importe).toBe(8.96);
  });

  it('campos vacíos cuentan como cero', () => {
    const r = calcularEncargo(tarifas(true, true), [{ cantidad: null, horasImpresion: null, horasManoObra: null, variosImporte: null, bobinas: [] }], null);
    expect(r.precioFinal).toBe(0);
  });
});
