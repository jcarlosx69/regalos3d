// Tipos que reflejan los DTO del backend (es.labjc.regalos3d.*).
// Los BigDecimal de Java llegan como number; las fechas LocalDate como 'yyyy-MM-dd'.

// ------------------------------------------------------------------ clientes y modelos

export interface Cliente {
  id: number;
  telefono: string;
  nombre: string;
  email?: string;
  direccion?: string;
  notas?: string;
}

export interface ClienteRequest {
  telefono: string;
  nombre: string;
  email?: string | null;
  direccion?: string | null;
  notas?: string | null;
}

export interface Modelo {
  id: number;
  manyfoldModelId: string;
  nombre: string;
  gramosEstimados?: number;
  manyfoldUrl: string;
}

export interface ModeloRequest {
  manyfoldModelId: string;
  nombre: string;
  gramosEstimados?: number | null;
}

// ------------------------------------------------------------------ ajustes

/** Tarifas del taller. Los encargos las copian al crearse. */
export interface Tarifas {
  precioKg: number;
  precioKwh: number;
  potenciaW: number;
  manoObraHora: number;
  amortizacionHora: number;
  margenPct: number;
}

/** Ajustes de los documentos PDF. */
export interface AjustesDocumentos {
  validezPresupuestoDias: number;
  /** Texto libre al pie del presupuesto y del albarán. */
  textoPie?: string | null;
}

export type TipoDocumento = 'presupuesto' | 'albaran' | 'etiqueta';

// ------------------------------------------------------------------ encargos

export type TipoEncargo = 'VENTA' | 'REGALO';

export type EstadoEncargo =
  | 'PRESUPUESTO'
  | 'ACEPTADO'
  | 'EN_COLA'
  | 'IMPRIMIENDO'
  | 'TERMINADO'
  | 'ENTREGADO'
  | 'COBRADO'
  | 'CANCELADO';

/** Gramos por unidad. precioKg vacío: el backend aplica el de los ajustes. */
export interface FilamentoRequest {
  material: string;
  color?: string | null;
  gramos: number;
  precioKg: number | null;
}

/** Horas, gramos y varios son por unidad. */
export interface LineaRequest {
  modeloId: number;
  cantidad: number;
  horasImpresion?: number | null;
  horasManoObra?: number | null;
  variosConcepto?: string | null;
  variosImporte?: number | null;
  filamentos: FilamentoRequest[];
  /** Regalo o promoción dentro del encargo: a 0 €, aunque su coste cuenta. */
  obsequio: boolean;
  /** Precio por unidad fijado a mano; null = el calculado. Se ignora en un obsequio. */
  precioManual?: number | null;
}

export interface EncargoRequest {
  clienteId: number;
  tipo: TipoEncargo;
  estado?: EstadoEncargo | null;
  fecha: string;
  ocasion?: string | null;
  notas?: string | null;
  incluirManoObra: boolean;
  incluirMargen: boolean;
  potenciaW?: number | null;
  precioKwh?: number | null;
  manoObraHora?: number | null;
  amortizacionHora?: number | null;
  margenPct?: number | null;
  /** Precio final fijado a mano; null = el calculado. */
  precioManual?: number | null;
  lineas: LineaRequest[];
}

export interface Filamento {
  id: number;
  material: string;
  color?: string;
  gramos: number;
  precioKg: number;
}

/** Importes por unidad (desglose) más el importe de la línea. */
export interface LineaEncargo {
  id: number;
  posicion: number;
  modelo: { id: number; nombre: string; manyfoldModelId: string };
  cantidad: number;
  horasImpresion?: number;
  horasManoObra?: number;
  variosConcepto?: string;
  variosImporte?: number;
  filamentos: Filamento[];
  obsequio: boolean;
  precioManual?: number | null;
  material: number;
  energia: number;
  amortizacion: number;
  costeUnitario: number;
  manoObraUnitaria: number;
  margenUnitario: number;
  /** Precio por unidad según las tarifas. */
  precioCalculado: number;
  /** Precio por unidad que se cobra: 0 en un obsequio, o el manual si lo hay. */
  precioUnitario: number;
  importe: number;
}

export interface TarifasEncargo {
  potenciaW: number;
  precioKwh: number;
  manoObraHora: number;
  amortizacionHora: number;
  margenPct: number;
}

export interface Encargo {
  id: number;
  referencia: string;
  cliente: { id: number; nombre: string; telefono: string; email?: string; direccion?: string };
  tipo: TipoEncargo;
  estado: EstadoEncargo;
  fecha: string;
  fechaEntrega?: string;
  fechaCobro?: string;
  ocasion?: string;
  notas?: string;
  incluirManoObra: boolean;
  incluirMargen: boolean;
  tarifas: TarifasEncargo;
  lineas: LineaEncargo[];
  coste: number;
  manoObra: number;
  margen: number;
  precioCalculado: number;
  precioManual?: number;
  precioFinal: number;
  beneficio: number;
}

export interface EncargoResumen {
  id: number;
  referencia: string;
  clienteId: number;
  clienteNombre: string;
  tipo: TipoEncargo;
  estado: EstadoEncargo;
  fecha: string;
  fechaEntrega?: string;
  ocasion?: string;
  articulos: { modelo: string; cantidad: number; obsequio: boolean }[];
  unidades: number;
  coste: number;
  precioFinal: number;
  precioAjustado: boolean;
}

/** Encargo anterior en el que el cliente ya recibió uno de los modelos. */
export interface EncargoPrevio {
  encargoId: number;
  referencia: string;
  fecha: string;
  tipo: TipoEncargo;
  ocasion?: string;
  modeloId: number;
  modelo: string;
}

export interface ComprobacionDuplicado {
  duplicado: boolean;
  previos: EncargoPrevio[];
}

// ------------------------------------------------------------------ informes

export interface Resumen {
  ventas: number;
  facturado: number;
  cobrado: number;
  pendienteCobro: number;
  beneficio: number;
  regalos: number;
  costeRegalos: number;
  valorRegalos: number;
  enCurso: number;
  importeEnCurso: number;
  presupuestos: number;
  importePresupuestos: number;
}

export interface ImporteCliente {
  clienteId: number;
  nombre: string;
  encargos: number;
  facturado: number;
  beneficio: number;
  costeRegalos: number;
}

export interface ImporteAnio {
  anio: number;
  ventas: number;
  facturado: number;
  beneficio: number;
  regalos: number;
  costeRegalos: number;
}

export interface ImporteOcasion {
  ocasion: string;
  encargos: number;
  importe: number;
}

export interface ModeloEncargado {
  modeloId: number;
  nombre: string;
  manyfoldModelId: string;
  unidades: number;
  encargos: number;
  clientes: number;
}

/** Cuerpo de error del backend (ProblemDetail, RFC 9457). */
export interface Problema {
  status: number;
  title?: string;
  detail?: string;
  codigo?: string;
  campos?: Record<string, string>;
  previos?: EncargoPrevio[];
}
