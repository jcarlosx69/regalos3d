import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { Api } from '../../core/api';
import { GrupoEncargos, enGrupo } from '../../core/estados';
import { EncargoResumen, TipoEncargo } from '../../core/modelos-api';
import { Avisos } from '../../shared/avisos';
import { TablaEncargos } from './tabla-encargos';

const GRUPOS: { id: GrupoEncargos; nombre: string }[] = [
  { id: 'activos', nombre: 'Activos' },
  { id: 'por-cobrar', nombre: 'Por cobrar' },
  { id: 'finalizados', nombre: 'Finalizados' },
  { id: 'cancelados', nombre: 'Cancelados' },
  { id: 'todos', nombre: 'Todos' },
];

@Component({
  selector: 'app-encargos-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, MatButtonModule, MatButtonToggleModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatProgressBarModule, TablaEncargos,
  ],
  template: `
    <div class="pagina">
      <header class="pagina-cabecera">
        <div>
          <h1>Encargos</h1>
          <p>Ventas y regalos, desde el presupuesto hasta la entrega y el cobro.</p>
        </div>
        <a mat-flat-button routerLink="/encargos/nuevo"><mat-icon>add</mat-icon> Nuevo encargo</a>
      </header>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (encargos().length) {
        <mat-button-toggle-group class="grupos" [value]="grupo()" (change)="grupo.set($event.value)"
                                 aria-label="Estado de los encargos" hideSingleSelectionIndicator>
          @for (g of grupos; track g.id) {
            <mat-button-toggle [value]="g.id">
              {{ g.nombre }} <span class="cuenta cifra">{{ cuentas()[g.id] }}</span>
            </mat-button-toggle>
          }
        </mat-button-toggle-group>

        <div class="filtros">
            <mat-form-field appearance="outline" class="buscador">
              <mat-icon matPrefix>search</mat-icon>
              <mat-label>Buscar por referencia, cliente, modelo u ocasión</mat-label>
              <input matInput [value]="texto()" (input)="texto.set($any($event.target).value)" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="tipo">
              <mat-label>Tipo</mat-label>
              <mat-select [value]="tipo()" (selectionChange)="tipo.set($event.value)">
                <mat-option value="TODOS">Ventas y regalos</mat-option>
                <mat-option value="VENTA">Solo ventas</mat-option>
                <mat-option value="REGALO">Solo regalos</mat-option>
              </mat-select>
            </mat-form-field>
            <span class="recuento">{{ filtrados().length }} de {{ encargos().length }} encargos</span>
        </div>

        @if (filtrados().length) {
          <app-tabla-encargos [encargos]="filtrados()" (cambiado)="cargar()" />
        } @else {
          <div class="vacio">
            <h2>No hay encargos en esta vista</h2>
            <p>Prueba con otra pestaña o cambia los filtros de búsqueda y tipo.</p>
          </div>
        }
      } @else {
        <div class="vacio">
          <h2>No hay encargos registrados</h2>
          <p>
            Un encargo agrupa uno o varios modelos para un cliente, como venta o como regalo. Se sigue desde
            el presupuesto hasta la entrega, con su coste y su precio de venta.
          </p>
          <a mat-flat-button routerLink="/encargos/nuevo"><mat-icon>add</mat-icon> Nuevo encargo</a>
        </div>
      }
    </div>
  `,
  styles: `
    .grupos { margin-bottom: 16px; flex-wrap: wrap; }
    .cuenta {
      display: inline-block; min-width: 20px; margin-left: 4px; padding: 0 6px; border-radius: 999px;
      background: rgba(26, 31, 41, 0.08); font-size: 0.75rem; font-weight: 600;
    }
    .filtros { display: flex; gap: 12px; flex-wrap: wrap; align-items: flex-start; }
    .buscador { flex: 0 1 400px; min-width: 240px; }
    .tipo { flex: 0 0 200px; }
    .recuento { margin-left: auto; padding-top: 16px; color: var(--tinta-suave); font-size: 0.875rem; }
  `,
})
export class EncargosLista {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  protected readonly grupos = GRUPOS;
  protected readonly encargos = signal<EncargoResumen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly grupo = signal<GrupoEncargos>('activos');
  protected readonly tipo = signal<TipoEncargo | 'TODOS'>('TODOS');
  protected readonly texto = signal('');

  /** Encargos que pasan los filtros de tipo y texto (antes de separar por pestaña). */
  private readonly buscados = computed(() => {
    const t = this.texto().trim().toLowerCase();
    const tipo = this.tipo();
    return this.encargos().filter(
      (e) =>
        (tipo === 'TODOS' || e.tipo === tipo) &&
        (!t ||
          [e.referencia, e.clienteNombre, e.ocasion ?? '', ...e.articulos.map((a) => a.modelo)].some((x) =>
            x.toLowerCase().includes(t),
          )),
    );
  });

  protected readonly cuentas = computed(() => {
    const r = {} as Record<GrupoEncargos, number>;
    for (const g of GRUPOS) r[g.id] = this.buscados().filter((e) => enGrupo(g.id, e.estado, e.tipo)).length;
    return r;
  });

  protected readonly filtrados = computed(() =>
    this.buscados().filter((e) => enGrupo(this.grupo(), e.estado, e.tipo)),
  );

  constructor() {
    this.cargar(true);
  }

  protected cargar(inicial = false): void {
    this.api.encargos().subscribe({
      next: (es) => {
        this.encargos.set(es);
        this.cargando.set(false);
        // Primera visita sin nada activo: se muestran todos en lugar de una pestaña vacía
        if (inicial && !es.some((e) => enGrupo('activos', e.estado, e.tipo))) this.grupo.set('todos');
      },
      error: (e) => {
        this.cargando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
