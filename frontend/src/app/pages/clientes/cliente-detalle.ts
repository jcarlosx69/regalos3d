import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Api } from '../../core/api';
import { Cliente, EncargoResumen } from '../../core/modelos-api';
import { Avisos } from '../../shared/avisos';
import { EurosPipe, TelefonoPipe } from '../../shared/formato';
import { TablaEncargos } from '../encargos/tabla-encargos';
import { abrirClienteDialog } from './cliente-dialog';

/** Ficha de un cliente con su historial de encargos. */
@Component({
  selector: 'app-cliente-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, TablaEncargos, EurosPipe, TelefonoPipe],
  template: `
    <div class="pagina">
      @if (cliente(); as c) {
        <header class="pagina-cabecera">
          <div>
            <nav class="miga" aria-label="Ruta">
              <a routerLink="/clientes">Clientes</a>
              <mat-icon inline>chevron_right</mat-icon>
              <span>{{ c.nombre }}</span>
            </nav>
            <h1>{{ c.nombre }}</h1>
          </div>
          <div class="botones">
            <button mat-stroked-button (click)="editar(c)"><mat-icon>edit</mat-icon> Editar</button>
            <a mat-flat-button routerLink="/encargos/nuevo" [queryParams]="{ cliente: c.id }">
              <mat-icon>add</mat-icon> Nuevo encargo
            </a>
          </div>
        </header>

        <dl class="ficha">
          <div>
            <dt>Móvil</dt>
            <dd class="cifra">{{ c.telefono | telefono }}</dd>
          </div>
          <div>
            <dt>Email</dt>
            <dd>
              @if (c.email) { <a [href]="'mailto:' + c.email">{{ c.email }}</a> } @else { <span class="suave">—</span> }
            </dd>
          </div>
          <div>
            <dt>Facturado</dt>
            <dd class="cifra">{{ facturado() | euros }}</dd>
          </div>
          <div>
            <dt>Coste en regalos</dt>
            <dd class="cifra">{{ costeRegalos() | euros }}</dd>
          </div>
          <div class="ancho">
            <dt>Dirección de entrega</dt>
            <dd class="normal">{{ c.direccion || '—' }}</dd>
          </div>
          @if (c.notas) {
            <div class="ancho">
              <dt>Notas</dt>
              <dd class="normal">{{ c.notas }}</dd>
            </div>
          }
        </dl>

        <section aria-labelledby="titulo-encargos">
          <h2 id="titulo-encargos">Encargos</h2>
          @if (encargos().length) {
            <app-tabla-encargos [encargos]="encargos()" [mostrarCliente]="false" (cambiado)="cargar(c.id)" />
          } @else {
            <div class="vacio">
              <h2>Sin encargos</h2>
              <p>Las ventas y los regalos de este cliente aparecerán aquí.</p>
            </div>
          }
        </section>
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </div>
  `,
  styles: `
    .ficha {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      margin: 0 0 28px;
      background: var(--superficie);
      border: 1px solid var(--linea);
      border-radius: var(--radio-panel);

      > div { padding: 12px 16px; border-left: 1px solid var(--linea); min-width: 0; }
      > div:first-child { border-left: none; }
      dt { font-size: 0.8125rem; color: var(--tinta-suave); }
      dd { margin: 2px 0 0; font-size: 1.05rem; font-weight: 600; overflow-wrap: anywhere; }
      dd.normal { font-weight: 400; font-size: 0.95rem; max-width: 80ch; white-space: pre-line; }
      .ancho { grid-column: 1 / -1; border-left: none; border-top: 1px solid var(--linea); }
    }
    section h2 { margin-bottom: 12px; }
    @media (max-width: 720px) {
      .ficha { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .ficha > div:nth-child(3) { border-left: none; }
      .ficha > div:nth-child(n + 3) { border-top: 1px solid var(--linea); }
    }
  `,
})
export class ClienteDetalle {
  private readonly api = inject(Api);
  private readonly dialog = inject(MatDialog);
  private readonly avisos = inject(Avisos);

  /** Parámetro :id de la ruta. */
  readonly id = input.required<string>();

  protected readonly cliente = signal<Cliente | null>(null);
  protected readonly encargos = signal<EncargoResumen[]>([]);

  private realizados(tipo: 'VENTA' | 'REGALO') {
    return this.encargos().filter((e) => e.tipo === tipo && (e.estado === 'ENTREGADO' || e.estado === 'COBRADO'));
  }
  protected readonly facturado = computed(() => this.realizados('VENTA').reduce((s, e) => s + e.precioFinal, 0));
  protected readonly costeRegalos = computed(() => this.realizados('REGALO').reduce((s, e) => s + e.coste, 0));

  constructor() {
    effect(() => this.cargar(Number(this.id())));
  }

  protected cargar(id: number): void {
    forkJoin([this.api.cliente(id), this.api.encargos(id)]).subscribe({
      next: ([c, es]) => {
        this.cliente.set(c);
        this.encargos.set(es);
      },
      error: (e) => this.avisos.error(e),
    });
  }

  protected editar(c: Cliente): void {
    abrirClienteDialog(this.dialog, c).subscribe((g) => {
      if (g) {
        this.cliente.set(g);
        this.avisos.hecho('Cambios guardados');
      }
    });
  }
}
