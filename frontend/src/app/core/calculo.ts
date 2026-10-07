/**
 * Mismo cálculo que CalculadoraPrecio.java, para el presupuesto en vivo del formulario.
 * Al guardar, el backend recalcula y sus importes son los que quedan registrados.
 *
 * Por unidad, cada componente redondeado a céntimos (mitad hacia arriba):
 *   material = Σ gramos × €/kg / 1000 · energía = h × W / 1000 × €/kWh · amortización = h × €/h
 *   coste = material + energía + amortización + varios
 *   mano de obra = h × €/h (si se incluye) · margen = (coste + mano de obra) × % (si se incluye)
 *   precio calculado = coste + mano de obra + margen
 *   precio por unidad = 0 si es obsequio; si no, el manual del artículo o el calculado
 *   importe = precio por unidad × cantidad
 */

export interface TarifasCalculo {
  potenciaW: number | null;
  precioKwh: number | null;
  manoObraHora: number | null;
  amortizacionHora: number | null;
  margenPct: number | null;
  incluirManoObra: boolean;
  incluirMargen: boolean;
}

export interface LineaCalculo {
  cantidad: number | null;
  horasImpresion: number | null;
  horasManoObra: number | null;
  variosImporte: number | null;
  bobinas: { gramos: number | null; precioKg: number | null }[];
  /** Regalo o promoción: no se cobra, pero su coste cuenta. */
  obsequio?: boolean;
  /** Precio por unidad fijado a mano; null = el calculado. */
  precioManual?: number | null;
}

export interface ResultadoLinea {
  material: number;
  energia: number;
  amortizacion: number;
  varios: number;
  costeUnitario: number;
  manoObraUnitaria: number;
  margenUnitario: number;
  /** Precio por unidad según las tarifas. */
  precioCalculado: number;
  /** Precio por unidad que se cobra. */
  precioUnitario: number;
  importe: number;
}

export interface ResultadoEncargo {
  lineas: ResultadoLinea[];
  coste: number;
  manoObra: number;
  margen: number;
  /** Suma de los importes de los artículos (con obsequios y precios manuales). */
  precioCalculado: number;
  precioFinal: number;
  beneficio: number;
  /** Lo que los obsequios y precios a mano de los artículos se apartan del cálculo (negativo: descuento). */
  ajusteArticulos: number;
}

/** Redondeo a céntimos sin errores de coma flotante (1,005 → 1,01). */
export function centimos(v: number): number {
  return Math.round(Number((v * 100).toPrecision(12))) / 100;
}

/** Suma de importes ya redondeados, sin arrastrar decimales binarios. */
function sumar(...v: number[]): number {
  return centimos(v.reduce((s, x) => s + x, 0));
}

const n = (v: number | null | undefined): number => (v == null || Number.isNaN(Number(v)) ? 0 : Number(v));

export function calcularLinea(t: TarifasCalculo, l: LineaCalculo): ResultadoLinea {
  const horas = n(l.horasImpresion);
  const material = sumar(...l.bobinas.map((b) => centimos((n(b.gramos) * n(b.precioKg)) / 1000)));
  const energia = centimos(((horas * n(t.potenciaW)) / 1000) * n(t.precioKwh));
  const amortizacion = centimos(horas * n(t.amortizacionHora));
  const varios = centimos(n(l.variosImporte));
  const costeUnitario = sumar(material, energia, amortizacion, varios);
  const manoObraUnitaria = t.incluirManoObra ? centimos(n(l.horasManoObra) * n(t.manoObraHora)) : 0;
  const base = sumar(costeUnitario, manoObraUnitaria);
  const margenUnitario = t.incluirMargen ? centimos((base * n(t.margenPct)) / 100) : 0;
  const precioCalculado = sumar(base, margenUnitario);
  const manual = l.precioManual == null || `${l.precioManual}` === '' ? null : centimos(n(l.precioManual));
  const precioUnitario = l.obsequio ? 0 : (manual ?? precioCalculado);
  const importe = centimos(precioUnitario * Math.max(0, Math.trunc(n(l.cantidad))));
  return {
    material, energia, amortizacion, varios, costeUnitario, manoObraUnitaria, margenUnitario, precioCalculado,
    precioUnitario, importe,
  };
}

export function calcularEncargo(t: TarifasCalculo, lineas: LineaCalculo[], precioManual: number | null): ResultadoEncargo {
  const resultados = lineas.map((l) => calcularLinea(t, l));
  const cant = (i: number) => Math.max(0, Math.trunc(n(lineas[i].cantidad)));
  const coste = sumar(...resultados.map((r, i) => r.costeUnitario * cant(i)));
  const manoObra = sumar(...resultados.map((r, i) => r.manoObraUnitaria * cant(i)));
  const margen = sumar(...resultados.map((r, i) => r.margenUnitario * cant(i)));
  const precioCalculado = sumar(...resultados.map((r) => r.importe));
  const precioFinal = precioManual != null ? centimos(precioManual) : precioCalculado;
  return {
    lineas: resultados, coste, manoObra, margen, precioCalculado, precioFinal,
    beneficio: centimos(precioFinal - coste),
    ajusteArticulos: centimos(precioCalculado - coste - manoObra - margen),
  };
}
