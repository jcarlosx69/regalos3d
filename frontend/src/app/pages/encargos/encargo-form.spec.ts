import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { Cliente, Modelo, Tarifas } from '../../core/modelos-api';
import { EncargoForm } from './encargo-form';

const lucia: Cliente = { id: 1, telefono: '+34611223344', nombre: 'Lucía' };
const llavero: Modelo = { id: 7, manyfoldModelId: 'll', nombre: 'Llavero', gramosEstimados: 8, manyfoldUrl: 'http://m/models/ll' };
const soporte: Modelo = { id: 8, manyfoldModelId: 'so', nombre: 'Soporte', gramosEstimados: 45, manyfoldUrl: 'http://m/models/so' };
const tarifas: Tarifas = { precioKg: 20, precioKwh: 0.15, potenciaW: 150, manoObraHora: 12, amortizacionHora: 0.25, margenPct: 30 };

describe('EncargoForm', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function crear() {
    const f = TestBed.createComponent(EncargoForm);
    f.detectChanges();
    TestBed.tick();
    http.expectOne('/api/clientes').flush([lucia]);
    http.expectOne('/api/modelos').flush([llavero, soporte]);
    http.expectOne('/api/ajustes').flush(tarifas);
    f.detectChanges();
    return { f, c: f.componentInstance as any };
  }

  it('un encargo nuevo es una venta en presupuesto con las tarifas de Ajustes y un artículo', () => {
    const { c } = crear();
    expect(c.form.controls.tipo.value).toBe('VENTA');
    expect(c.form.controls.estado.value).toBe('PRESUPUESTO');
    expect(c.form.controls.incluirMargen.value).toBe(true);
    expect(c.form.controls.margenPct.value).toBe(30);
    expect(c.articulos.length).toBe(1);
    expect(c.articulos.at(0).controls.bobinas.at(0).controls.precioKg.value).toBe(20);
  });

  it('al elegir el modelo, su peso estimado rellena la primera bobina', () => {
    const { c } = crear();
    c.articulos.at(0).controls.modelo.setValue(llavero);
    expect(c.articulos.at(0).controls.bobinas.at(0).controls.gramos.value).toBe(8);
  });

  it('calcula el presupuesto de varios artículos igual que el backend', () => {
    const { c } = crear();
    const a1 = c.articulos.at(0);
    a1.patchValue({ modelo: llavero, cantidad: 10, horasImpresion: 0.5, horasManoObra: 0.1 });
    c.anadirArticulo();
    const a2 = c.articulos.at(1);
    a2.patchValue({ modelo: soporte, cantidad: 1, horasImpresion: 3, horasManoObra: 0.25 });
    a2.controls.bobinas.at(0).patchValue({ material: 'PETG', gramos: 45, precioKg: 24 });

    const p = c.presupuesto();
    expect(p.articulos.map((a: any) => a.importe)).toEqual([19.5, 6.37]);
    expect(p.coste).toBe(4.9);
    expect(p.precioCalculado).toBe(25.87);
    expect(p.beneficio).toBe(20.97);

    c.form.controls.precioManual.setValue(30);
    expect(c.presupuesto().precioFinal).toBe(30);
    expect(c.presupuesto().ajuste).toBe(4.13);
  });

  it('un regalo pasa a calcularse al coste y empieza en cola', () => {
    const { c } = crear();
    c.cambiarTipo('REGALO');
    expect(c.form.controls.incluirManoObra.value).toBe(false);
    expect(c.form.controls.incluirMargen.value).toBe(false);
    expect(c.form.controls.estado.value).toBe('EN_COLA');
    expect(c.flujo()).not.toContain('COBRADO');
  });

  it('avisa si el cliente ya recibió el modelo de un regalo', async () => {
    const { f, c } = crear();
    c.cambiarTipo('REGALO');
    c.form.controls.cliente.setValue(lucia);
    c.articulos.at(0).controls.modelo.setValue(llavero);
    await new Promise((r) => setTimeout(r, 250)); // debounce de la comprobación

    http
      .expectOne((r) => r.url === '/api/encargos/comprobar' && r.params.get('clienteId') === '1' && r.params.get('modeloIds') === '7')
      .flush({ duplicado: true, previos: [{ encargoId: 3, referencia: 'E-2026-0003', fecha: '2026-03-12', tipo: 'REGALO', modeloId: 7, modelo: 'Llavero' }] });
    f.detectChanges();

    expect(c.previos().length).toBe(1);
    expect(f.nativeElement.querySelector('.aviso-repetido')?.textContent).toContain('Lucía ya recibió');
  });

  it('envía artículos, bobinas, tarifas y precio final al guardar', () => {
    const { c } = crear();
    c.form.controls.cliente.setValue(lucia);
    c.articulos.at(0).patchValue({ modelo: llavero, cantidad: 3, variosConcepto: ' Anilla ', variosImporte: 0.2 });
    c.form.controls.precioManual.setValue(12);

    const req = c.peticion();
    expect(req.clienteId).toBe(1);
    expect(req.estado).toBe('PRESUPUESTO');
    expect(req.margenPct).toBe(30);
    expect(req.precioManual).toBe(12);
    expect(req.lineas).toEqual([
      {
        modeloId: 7, cantidad: 3, horasImpresion: null, horasManoObra: null, variosConcepto: 'Anilla', variosImporte: 0.2,
        obsequio: false, precioManual: null,
        filamentos: [{ material: 'PLA', color: null, gramos: 8, precioKg: 20 }],
      },
    ]);
  });

  it('un artículo con precio a mano y otro de obsequio', () => {
    const { c } = crear();
    c.articulos.at(0).patchValue({ modelo: llavero, cantidad: 10, horasImpresion: 0.5, horasManoObra: 0.1, precioManual: 2.5 });
    c.articulos.at(0).controls.bobinas.at(0).patchValue({ gramos: 8 });
    c.anadirArticulo();
    const a2 = c.articulos.at(1);
    a2.patchValue({ cantidad: 1, horasImpresion: 3, horasManoObra: 0.25, precioManual: 9 });
    a2.controls.bobinas.at(0).patchValue({ material: 'PETG', gramos: 45, precioKg: 24 });
    a2.controls.obsequio.setValue(true);

    const p = c.presupuesto();
    expect(a2.controls.precioManual.value).toBeNull();
    expect(p.articulos.map((a: any) => a.importe)).toEqual([25, 0]);
    expect(p.articulos[1].coste).toBe(1.9);
    expect(p.precioFinal).toBe(25);
    expect(p.beneficio).toBe(20.1);
    expect(p.ajusteArticulos).toBe(-0.87); // +5,50 del precio a mano − 6,37 del obsequio
  });

  it('al pasar a regalo se quitan los obsequios', () => {
    const { c } = crear();
    c.articulos.at(0).controls.obsequio.setValue(true);
    c.cambiarTipo('REGALO');
    expect(c.articulos.at(0).controls.obsequio.value).toBe(false);
  });

  it('no deja quitar el único artículo', () => {
    const { c } = crear();
    c.quitarArticulo(0);
    expect(c.articulos.length).toBe(1);
  });
});
