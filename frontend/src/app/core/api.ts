import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  AjustesDocumentos, Cliente, ClienteRequest, ComprobacionDuplicado, Encargo, EncargoRequest, EncargoResumen, EstadoEncargo,
  ImporteAnio, ImporteCliente, ImporteOcasion, Modelo, ModeloEncargado, ModeloRequest, Resumen, Tarifas, TipoDocumento,
} from './modelos-api';

/** Acceso a la API REST. Rutas relativas: en desarrollo las reenvía el proxy de ng serve. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  // ------------------------------------------------------------ clientes
  clientes(q?: string) {
    const params = q ? new HttpParams().set('q', q) : undefined;
    return this.http.get<Cliente[]>('/api/clientes', { params });
  }
  cliente(id: number) {
    return this.http.get<Cliente>(`/api/clientes/${id}`);
  }
  crearCliente(c: ClienteRequest) {
    return this.http.post<Cliente>('/api/clientes', c);
  }
  actualizarCliente(id: number, c: ClienteRequest) {
    return this.http.put<Cliente>(`/api/clientes/${id}`, c);
  }
  borrarCliente(id: number) {
    return this.http.delete<void>(`/api/clientes/${id}`);
  }

  // ------------------------------------------------------------ modelos
  modelos(q?: string) {
    const params = q ? new HttpParams().set('q', q) : undefined;
    return this.http.get<Modelo[]>('/api/modelos', { params });
  }
  crearModelo(m: ModeloRequest) {
    return this.http.post<Modelo>('/api/modelos', m);
  }
  actualizarModelo(id: number, m: ModeloRequest) {
    return this.http.put<Modelo>(`/api/modelos/${id}`, m);
  }
  borrarModelo(id: number) {
    return this.http.delete<void>(`/api/modelos/${id}`);
  }

  // ------------------------------------------------------------ encargos
  encargos(clienteId?: number) {
    const params = clienteId ? new HttpParams().set('clienteId', clienteId) : undefined;
    return this.http.get<EncargoResumen[]>('/api/encargos', { params });
  }
  encargo(id: number) {
    return this.http.get<Encargo>(`/api/encargos/${id}`);
  }
  comprobarDuplicado(clienteId: number, modeloIds: number[], excluir?: number | null) {
    let params = new HttpParams().set('clienteId', clienteId).set('modeloIds', modeloIds.join(','));
    if (excluir) params = params.set('excluir', excluir);
    return this.http.get<ComprobacionDuplicado>('/api/encargos/comprobar', { params });
  }
  crearEncargo(e: EncargoRequest, confirmarDuplicado = false) {
    const params = new HttpParams().set('confirmarDuplicado', confirmarDuplicado);
    return this.http.post<Encargo>('/api/encargos', e, { params });
  }
  actualizarEncargo(id: number, e: EncargoRequest, confirmarDuplicado = false) {
    const params = new HttpParams().set('confirmarDuplicado', confirmarDuplicado);
    return this.http.put<Encargo>(`/api/encargos/${id}`, e, { params });
  }
  cambiarEstado(id: number, estado: EstadoEncargo) {
    return this.http.patch<Encargo>(`/api/encargos/${id}/estado`, { estado });
  }
  borrarEncargo(id: number) {
    return this.http.delete<void>(`/api/encargos/${id}`);
  }

  /** PDF de un encargo. Se abre en otra pestaña: la cookie de sesión viaja sola en un GET del mismo origen. */
  urlDocumento(id: number, tipo: TipoDocumento): string {
    return `/api/encargos/${id}/${tipo}.pdf`;
  }

  // ------------------------------------------------------------ ajustes
  ajustes() {
    return this.http.get<Tarifas>('/api/ajustes');
  }
  guardarAjustes(t: Tarifas) {
    return this.http.put<Tarifas>('/api/ajustes', t);
  }
  ajustesDocumentos() {
    return this.http.get<AjustesDocumentos>('/api/ajustes/documentos');
  }
  guardarAjustesDocumentos(d: AjustesDocumentos) {
    return this.http.put<AjustesDocumentos>('/api/ajustes/documentos', d);
  }

  // ------------------------------------------------------------ informes
  private conAnio(anio?: number | null) {
    return anio ? new HttpParams().set('anio', anio) : undefined;
  }
  resumen(anio?: number | null) {
    return this.http.get<Resumen>('/api/informes/resumen', { params: this.conAnio(anio) });
  }
  porCliente(anio?: number | null) {
    return this.http.get<ImporteCliente[]>('/api/informes/por-cliente', { params: this.conAnio(anio) });
  }
  porAnio() {
    return this.http.get<ImporteAnio[]>('/api/informes/por-anio');
  }
  porOcasion(anio?: number | null) {
    return this.http.get<ImporteOcasion[]>('/api/informes/por-ocasion', { params: this.conAnio(anio) });
  }
  modelosEncargados(limite = 10) {
    return this.http.get<ModeloEncargado[]>('/api/informes/modelos', {
      params: new HttpParams().set('limite', limite),
    });
  }
}
