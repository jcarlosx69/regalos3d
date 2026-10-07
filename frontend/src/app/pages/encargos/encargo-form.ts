import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl, FormArray, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators,
} from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, Observable, catchError, debounceTime, distinctUntilChanged, forkJoin, map, of, switchMap } from 'rxjs';
import { Api } from '../../core/api';
import { calcularEncargo } from '../../core/calculo';
import { NOMBRE_ESTADO, flujoDe } from '../../core/estados';
import {
  Cliente, Encargo, EncargoPrevio, EncargoRequest, EstadoEncargo, Modelo, Problema, Tarifas, TipoDocumento,
  TipoEncargo,
} from '../../core/modelos-api';
import { Avisos } from '../../shared/avisos';
import { confirmar } from '../../shared/confirmar';
import { EurosPipe, FechaPipe, NumeroPipe, fechaCorta, hoyIso } from '../../shared/formato';
import { MuestraColor } from '../../shared/muestra-color';
import { abrirModeloDialog } from '../modelos/modelo-dialog';
import { abrirClienteDialog } from '../clientes/cliente-dialog';

const OCASIONES = ['Cumpleaños', 'Navidad', 'Reyes', 'Santo', 'Aniversario', 'Amigo invisible', 'Boda', 'Encargo de empresa'];
const MATERIALES = ['PLA', 'PLA Silk', 'PLA Mate', 'PETG', 'TPU', 'ABS', 'ASA'];
const COLORES = ['Blanco', 'Negro', 'Gris', 'Rojo', 'Rosa', 'Naranja', 'Amarillo', 'Verde', 'Azul', 'Morado',
  'Marrón', 'Dorado', 'Plateado', 'Transparente'];

type Bobina = FormGroup<{
  material: FormControl<string>;
  color: FormControl<string>;
  gramos: FormControl<number | null>;
  precioKg: FormControl<number | null>;
}>;

type Articulo = FormGroup<{
  modelo: FormControl<Modelo | string | null>;
  cantidad: FormControl<number | null>;
  horasImpresion: FormControl<number | null>;
  horasManoObra: FormControl<number | null>;
  variosConcepto: FormControl<string>;
  variosImporte: FormControl<number | null>;
  bobinas: FormArray<Bobina>;
  obsequio: FormControl<boolean>;
  precioManual: FormControl<number | null>;
}>;

/** El campo de autocompletado solo es válido si se eligió un elemento de la lista (no texto suelto). */
function elegidoDeLaLista(c: AbstractControl): ValidationErrors | null {
  const v = c.value;
  if (v == null || v === '') return { required: true };
  return typeof v === 'object' && 'id' in v ? null : { noElegido: true };
}

function filtrar(lista: string[], texto: string | null | undefined): string[] {
  const t = (texto ?? '').trim().toLowerCase();
  return t ? lista.filter((x) => x.toLowerCase().includes(t)) : lista;
}

const esObjeto = <T extends { id: number }>(v: T | string | null | undefined): v is T =>
  typeof v === 'object' && v !== null && 'id' in v;

@Component({
  selector: 'app-encargo-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatAutocompleteModule, MatButtonModule,
    MatButtonToggleModule, MatCheckboxModule, MatExpansionModule, MatIconModule, MatMenuModule, MatTooltipModule,
    MatProgressBarModule, EurosPipe, FechaPipe, NumeroPipe, MuestraColor,
  ],
  templateUrl: './encargo-form.html',
  styleUrl: './encargo-form.scss',
})
export class EncargoForm {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly avisos = inject(Avisos);

  /** :id de la ruta al abrir un encargo existente. */
  readonly id = input<string>();
  /** ?cliente=ID para llegar con el cliente elegido desde su ficha. */
  readonly cliente = input<string>();

  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly encargo = signal<Encargo | null>(null);
  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly modelos = signal<Modelo[]>([]);
  private tarifasTaller: Tarifas | null = null;

  protected readonly nombreEstado = NOMBRE_ESTADO;

  // ------------------------------------------------------------------ formulario
  protected readonly form = new FormGroup({
    cliente: new FormControl<Cliente | string | null>(null, elegidoDeLaLista),
    tipo: new FormControl<TipoEncargo>('VENTA', { nonNullable: true }),
    estado: new FormControl<EstadoEncargo>('PRESUPUESTO', { nonNullable: true }),
    fecha: new FormControl(hoyIso(), { nonNullable: true, validators: Validators.required }),
    ocasion: new FormControl('', { nonNullable: true, validators: Validators.maxLength(100) }),
    notas: new FormControl('', { nonNullable: true, validators: Validators.maxLength(1000) }),
    incluirManoObra: new FormControl(true, { nonNullable: true }),
    incluirMargen: new FormControl(true, { nonNullable: true }),
    potenciaW: new FormControl<number | null>(null, [Validators.required, Validators.min(0.1)]),
    precioKwh: new FormControl<number | null>(null, [Validators.required, Validators.min(0.0001)]),
    manoObraHora: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    amortizacionHora: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
    margenPct: new FormControl<number | null>(null, [Validators.required, Validators.min(0), Validators.max(999)]),
    precioManual: new FormControl<number | null>(null, Validators.min(0)),
    articulos: new FormArray<Articulo>([], Validators.required),
  });
  protected readonly articulos = this.form.controls.articulos;

  private readonly valor = toSignal(this.form.valueChanges.pipe(map(() => this.form.getRawValue())), {
    initialValue: this.form.getRawValue(),
  });

  protected readonly tipo = computed(() => this.valor().tipo);
  protected readonly flujo = computed(() => flujoDe(this.tipo()));
  protected readonly editando = computed(() => !!this.encargo());

  // ------------------------------------------------------------------ autocompletados
  private readonly textoCliente = toSignal(this.form.controls.cliente.valueChanges, { initialValue: null });
  protected readonly clientesFiltrados = computed(() => {
    const t = this.textoCliente();
    const q = typeof t === 'string' ? t.trim().toLowerCase() : '';
    return q
      ? this.clientes().filter((c) => c.nombre.toLowerCase().includes(q) || c.telefono.includes(q.replace(/\s/g, '')))
      : this.clientes();
  });
  protected readonly clienteElegido = computed(() => {
    const c = this.valor().cliente;
    return esObjeto<Cliente>(c) ? c : null;
  });

  protected modelosPara(texto: Modelo | string | null): Modelo[] {
    const q = typeof texto === 'string' ? texto.trim().toLowerCase() : '';
    return q ? this.modelos().filter((m) => m.nombre.toLowerCase().includes(q)) : this.modelos();
  }
  protected ocasionesPara(texto: string): string[] { return filtrar(OCASIONES, texto); }
  protected materialesPara(texto: string): string[] { return filtrar(MATERIALES, texto); }
  protected coloresPara(texto: string): string[] { return filtrar(COLORES, texto); }

  protected readonly nombreCliente = (c: Cliente | string | null) => (esObjeto<Cliente>(c) ? c.nombre : c ?? '');
  protected readonly nombreModelo = (m: Modelo | string | null) => (esObjeto<Modelo>(m) ? m.nombre : m ?? '');
  protected modeloDe(a: Articulo): Modelo | null {
    const m = a.controls.modelo.value;
    return esObjeto<Modelo>(m) ? m : null;
  }

  // ------------------------------------------------------------------ aviso de regalo repetido
  protected readonly previos = signal<EncargoPrevio[]>([]);

  // ------------------------------------------------------------------ presupuesto en vivo
  protected readonly presupuesto = computed(() => {
    const v = this.valor();
    const lineas = v.articulos.map((a) => ({
      cantidad: a.cantidad,
      horasImpresion: a.horasImpresion,
      horasManoObra: a.horasManoObra,
      variosImporte: a.variosImporte,
      bobinas: a.bobinas.map((b) => ({ gramos: b.gramos, precioKg: b.precioKg })),
      obsequio: a.obsequio,
      precioManual: a.precioManual,
    }));
    const manual = v.precioManual != null && `${v.precioManual}` !== '' ? Number(v.precioManual) : null;
    const r = calcularEncargo(v, lineas, manual);
    return {
      ...r,
      manual,
      ajuste: manual != null ? Math.round((manual - r.precioCalculado) * 100) / 100 : 0,
      margenPct: v.margenPct ?? 0,
      incluirManoObra: v.incluirManoObra,
      incluirMargen: v.incluirMargen,
      articulos: v.articulos.map((a, i) => ({
        nombre: esObjeto<Modelo>(a.modelo) ? a.modelo.nombre : `Artículo ${i + 1}`,
        cantidad: a.cantidad ?? 0,
        obsequio: a.obsequio,
        precioAMano: !a.obsequio && a.precioManual != null && `${a.precioManual}` !== '',
        precioCalculado: r.lineas[i].precioCalculado,
        precioUnitario: r.lineas[i].precioUnitario,
        importe: r.lineas[i].importe,
        coste: Math.round(r.lineas[i].costeUnitario * (a.cantidad ?? 0) * 100) / 100,
      })),
      porcentajeBeneficio: r.precioFinal > 0 ? Math.round((r.beneficio / r.precioFinal) * 100) : 0,
    };
  });

  protected readonly resumenTarifas = computed(() => {
    const v = this.valor();
    const f = (x: number | null, d = 2) => (x == null ? '—' : x.toLocaleString('es-ES', { maximumFractionDigits: d }));
    return `${f(v.potenciaW, 1)} W · ${f(v.precioKwh, 4)} €/kWh · ${f(v.manoObraHora)} €/h mano de obra · `
      + `${f(v.amortizacionHora)} €/h amortización · ${f(v.margenPct)} % margen`;
  });

  constructor() {
    effect(() => this.cargar(this.id(), this.cliente()));

    // Comprobación de regalo repetido: cliente + modelos elegidos, solo en regalos
    this.form.valueChanges
      .pipe(
        map(() => {
          const v = this.form.getRawValue();
          const cliente = esObjeto<Cliente>(v.cliente) ? v.cliente.id : null;
          const modelos = v.articulos.map((a) => (esObjeto<Modelo>(a.modelo) ? a.modelo.id : null))
            .filter((x): x is number => x != null);
          return v.tipo === 'REGALO' && cliente && modelos.length ? `${cliente}|${[...new Set(modelos)].join(',')}` : '';
        }),
        distinctUntilChanged(),
        debounceTime(200),
        switchMap((clave) => {
          if (!clave) return of(null);
          const [cliente, modelos] = clave.split('|');
          return this.api
            .comprobarDuplicado(Number(cliente), modelos.split(',').map(Number), this.encargo()?.id)
            .pipe(catchError(() => of(null)));
        }),
        takeUntilDestroyed(),
      )
      .subscribe((r) => this.previos.set(r?.previos ?? []));
  }

  // ------------------------------------------------------------------ carga
  private cargar(id: string | undefined, clienteId: string | undefined): void {
    this.cargando.set(true);
    const encargo$: Observable<Encargo | null> = id ? this.api.encargo(Number(id)) : of(null);
    forkJoin([this.api.clientes(), this.api.modelos(), this.api.ajustes(), encargo$]).subscribe({
      next: ([clientes, modelos, tarifas, encargo]) => {
        this.clientes.set(clientes);
        this.modelos.set(modelos);
        this.tarifasTaller = tarifas;
        if (encargo) this.rellenarDesde(encargo);
        else this.rellenarNuevo(tarifas, clienteId);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.avisos.error(e);
      },
    });
  }

  private rellenarNuevo(t: Tarifas, clienteId?: string): void {
    this.encargo.set(null);
    this.articulos.clear();
    this.form.reset({
      tipo: 'VENTA', estado: 'PRESUPUESTO', fecha: hoyIso(), ocasion: '', notas: '',
      incluirManoObra: true, incluirMargen: true,
      potenciaW: t.potenciaW, precioKwh: t.precioKwh, manoObraHora: t.manoObraHora,
      amortizacionHora: t.amortizacionHora, margenPct: t.margenPct, precioManual: null,
    });
    this.anadirArticulo();
    const c = clienteId ? this.clientes().find((x) => x.id === Number(clienteId)) : undefined;
    this.form.controls.cliente.setValue(c ?? null);
  }

  private rellenarDesde(e: Encargo): void {
    this.encargo.set(e);
    // Primero la cabecera: reset() vaciaría también los artículos si ya estuvieran cargados
    this.form.reset({
      cliente: this.clientes().find((c) => c.id === e.cliente.id) ?? null,
      tipo: e.tipo, estado: e.estado, fecha: e.fecha, ocasion: e.ocasion ?? '', notas: e.notas ?? '',
      incluirManoObra: e.incluirManoObra, incluirMargen: e.incluirMargen,
      potenciaW: e.tarifas.potenciaW, precioKwh: e.tarifas.precioKwh, manoObraHora: e.tarifas.manoObraHora,
      amortizacionHora: e.tarifas.amortizacionHora, margenPct: e.tarifas.margenPct,
      precioManual: e.precioManual ?? null,
    });
    this.articulos.clear();
    for (const l of e.lineas) {
      const a = this.nuevoArticulo(this.modelos().find((m) => m.id === l.modelo.id) ?? null, l.cantidad);
      a.patchValue({
        horasImpresion: l.horasImpresion ?? null,
        horasManoObra: l.horasManoObra ?? null,
        variosConcepto: l.variosConcepto ?? '',
        variosImporte: l.variosImporte ?? null,
        obsequio: l.obsequio,
        precioManual: l.precioManual ?? null,
      });
      for (const f of l.filamentos) a.controls.bobinas.push(this.nuevaBobina(f.material, f.color ?? '', f.gramos, f.precioKg));
      this.articulos.push(a);
    }
    this.form.markAsPristine();
  }

  // ------------------------------------------------------------------ artículos y bobinas
  private nuevaBobina(material = 'PLA', color = '', gramos: number | null = null, precioKg: number | null = null): Bobina {
    return new FormGroup({
      material: new FormControl(material, { nonNullable: true, validators: [Validators.required, Validators.maxLength(20)] }),
      color: new FormControl(color, { nonNullable: true, validators: Validators.maxLength(40) }),
      gramos: new FormControl<number | null>(gramos, [Validators.required, Validators.min(0.01)]),
      precioKg: new FormControl<number | null>(precioKg, [Validators.required, Validators.min(0.01)]),
    });
  }

  private nuevoArticulo(modelo: Modelo | null = null, cantidad = 1): Articulo {
    const a: Articulo = new FormGroup({
      modelo: new FormControl<Modelo | string | null>(modelo, elegidoDeLaLista),
      cantidad: new FormControl<number | null>(cantidad, [Validators.required, Validators.min(1), Validators.max(9999)]),
      horasImpresion: new FormControl<number | null>(null, Validators.min(0)),
      horasManoObra: new FormControl<number | null>(null, Validators.min(0)),
      variosConcepto: new FormControl('', { nonNullable: true, validators: Validators.maxLength(150) }),
      variosImporte: new FormControl<number | null>(null, Validators.min(0)),
      bobinas: new FormArray<Bobina>([]),
      obsequio: new FormControl(false, { nonNullable: true }),
      precioManual: new FormControl<number | null>(null, Validators.min(0)),
    });
    // Un obsequio va a 0 €: su precio a mano deja de tener sentido
    a.controls.obsequio.valueChanges.subscribe((o) => o && a.controls.precioManual.setValue(null));
    // Al elegir modelo, su peso estimado rellena la primera bobina si está vacía
    a.controls.modelo.valueChanges.subscribe((m) => {
      const primera = a.controls.bobinas.at(0);
      if (esObjeto<Modelo>(m) && m.gramosEstimados && primera && primera.controls.gramos.value == null) {
        primera.controls.gramos.setValue(m.gramosEstimados);
      }
    });
    return a;
  }

  protected anadirArticulo(): void {
    const a = this.nuevoArticulo();
    a.controls.bobinas.push(this.nuevaBobina('PLA', '', null, this.tarifasTaller?.precioKg ?? 20));
    this.articulos.push(a);
  }

  protected quitarArticulo(i: number): void {
    if (this.articulos.length > 1) this.articulos.removeAt(i);
  }

  /** Una bobina más: copia material y precio de la anterior (en multicolor suele ser el mismo filamento). */
  protected anadirBobina(a: Articulo): void {
    const b = a.controls.bobinas;
    const ultima = b.length ? b.at(b.length - 1).getRawValue() : null;
    b.push(this.nuevaBobina(ultima?.material ?? 'PLA', '', null, ultima?.precioKg ?? this.tarifasTaller?.precioKg ?? 20));
  }

  protected quitarBobina(a: Articulo, i: number): void {
    a.controls.bobinas.removeAt(i);
  }

  // ------------------------------------------------------------------ tipo, estado y precio
  /**
   * Venta: con mano de obra y margen. Regalo: al coste. Se puede cambiar después con las casillas.
   * Los obsequios solo tienen sentido en una venta: en un regalo ya no se cobra nada.
   */
  protected cambiarTipo(tipo: TipoEncargo): void {
    const venta = tipo === 'VENTA';
    this.form.patchValue({ tipo, incluirManoObra: venta, incluirMargen: venta });
    if (!venta) for (const a of this.articulos.controls) a.controls.obsequio.setValue(false);
    const estado = this.form.controls.estado.value;
    if (!this.editando()) {
      this.form.controls.estado.setValue(venta ? 'PRESUPUESTO' : 'EN_COLA');
    } else if (!venta && estado === 'COBRADO') {
      this.form.controls.estado.setValue('ENTREGADO');
    }
  }

  protected elegirEstado(estado: EstadoEncargo): void {
    this.form.controls.estado.setValue(estado);
    this.form.controls.estado.markAsDirty();
  }

  protected pasoAlcanzado(paso: EstadoEncargo): boolean {
    const actual = this.valor().estado;
    if (actual === 'CANCELADO') return false;
    return this.flujo().indexOf(paso) <= this.flujo().indexOf(actual);
  }

  protected usarCalculado(): void {
    this.form.controls.precioManual.setValue(null);
  }

  protected usarCalculadoArticulo(a: Articulo): void {
    a.controls.precioManual.setValue(null);
  }

  protected restaurarTarifas(): void {
    const t = this.tarifasTaller;
    if (!t) return;
    this.form.patchValue({
      potenciaW: t.potenciaW, precioKwh: t.precioKwh, manoObraHora: t.manoObraHora,
      amortizacionHora: t.amortizacionHora, margenPct: t.margenPct,
    });
  }

  // ------------------------------------------------------------------ altas rápidas
  protected nuevoCliente(): void {
    abrirClienteDialog(this.dialog).subscribe((c) => {
      if (!c) return;
      this.clientes.update((cs) => [...cs, c].sort((a, b) => a.nombre.localeCompare(b.nombre)));
      this.form.controls.cliente.setValue(c);
    });
  }

  protected nuevoModelo(a: Articulo): void {
    abrirModeloDialog(this.dialog).subscribe((m) => {
      if (!m) return;
      this.modelos.update((ms) => [...ms, m].sort((x, y) => x.nombre.localeCompare(y.nombre)));
      a.controls.modelo.setValue(m);
    });
  }

  // ------------------------------------------------------------------ guardar
  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.avisos.error(new Error('Revisa los campos marcados en rojo.'));
      return;
    }
    this.enviar(this.peticion(), false);
  }

  private peticion(): EncargoRequest {
    const v = this.form.getRawValue();
    return {
      clienteId: (v.cliente as Cliente).id,
      tipo: v.tipo,
      estado: v.estado,
      fecha: v.fecha,
      ocasion: v.ocasion.trim() || null,
      notas: v.notas.trim() || null,
      incluirManoObra: v.incluirManoObra,
      incluirMargen: v.incluirMargen,
      potenciaW: v.potenciaW,
      precioKwh: v.precioKwh,
      manoObraHora: v.manoObraHora,
      amortizacionHora: v.amortizacionHora,
      margenPct: v.margenPct,
      precioManual: v.precioManual,
      lineas: v.articulos.map((a) => ({
        modeloId: (a.modelo as Modelo).id,
        cantidad: a.cantidad as number,
        horasImpresion: a.horasImpresion,
        horasManoObra: a.horasManoObra,
        variosConcepto: a.variosConcepto.trim() || null,
        variosImporte: a.variosImporte,
        obsequio: a.obsequio,
        precioManual: a.obsequio ? null : a.precioManual,
        filamentos: a.bobinas.map((b) => ({
          material: b.material.trim(),
          color: b.color.trim() || null,
          gramos: b.gramos as number,
          precioKg: b.precioKg,
        })),
      })),
    };
  }

  private enviar(cuerpo: EncargoRequest, confirmado: boolean): void {
    const actual = this.encargo();
    const peticion$ = actual
      ? this.api.actualizarEncargo(actual.id, cuerpo, confirmado)
      : this.api.crearEncargo(cuerpo, confirmado);
    this.guardando.set(true);
    peticion$
      .pipe(
        catchError((e: unknown) => {
          this.guardando.set(false);
          const problema = e instanceof HttpErrorResponse ? (e.error as Problema | null) : null;
          if (problema?.codigo === 'REGALO_DUPLICADO') this.preguntarSiRepetir(cuerpo, problema);
          else this.avisos.error(e);
          return EMPTY;
        }),
      )
      .subscribe((guardado) => {
        this.guardando.set(false);
        if (actual) {
          this.rellenarDesde(guardado);
          this.avisos.hecho(`${guardado.referencia} guardado`);
        } else {
          this.avisos.hecho(`${guardado.referencia} registrado`);
          this.router.navigate(['/encargos', guardado.id], { replaceUrl: true });
        }
      });
  }

  private preguntarSiRepetir(cuerpo: EncargoRequest, problema: Problema): void {
    const lista = (problema.previos ?? [])
      .map((p) => `${p.modelo} (${p.referencia}, ${fechaCorta(p.fecha)})`)
      .join('; ');
    confirmar(this.dialog, {
      titulo: 'Regalo repetido',
      texto: `${this.clienteElegido()?.nombre ?? 'Este cliente'} ya recibió: ${lista}. ¿Guardar el regalo de todas formas?`,
      aceptar: 'Guardar igualmente',
    }).subscribe((ok) => ok && this.enviar(cuerpo, true));
  }

  // ------------------------------------------------------------------ ayudas para la plantilla
  protected urlDocumento(id: number, tipo: TipoDocumento): string {
    return this.api.urlDocumento(id, tipo);
  }

  protected error(control: AbstractControl, clave: string): boolean {
    return control.touched && control.hasError(clave);
  }
}
