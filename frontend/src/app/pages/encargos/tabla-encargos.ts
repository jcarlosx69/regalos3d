import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { Api } from '../../core/api';
import { NOMBRE_ESTADO, NOMBRE_TIPO, sePuedeEliminar, siguienteEstado } from '../../core/estados';
import { EncargoResumen, TipoDocumento } from '../../core/modelos-api';
import { Avisos } from '../../shared/avisos';
import { EstadoChip } from '../../shared/estado-chip';
import { EurosPipe, FechaPipe } from '../../shared/formato';
import { AccionesEncargo } from './acciones-encargo';

/** Tabla de encargos con acciones rápidas de estado. Se usa en la lista general y en la ficha del cliente. */
@Component({
  selector: 'app-tabla-encargos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule, EurosPipe, FechaPipe, EstadoChip],
  template: `
    <div class="tabla-scroll">
      <table class="tabla">
        <thead>
          <tr>
            <th>Referencia</th>
            <th>Fecha</th>
            @if (mostrarCliente()) { <th>Cliente</th> }
            <th>Tipo</th>
            <th>Artículos</th>
            <th>Estado</th>
            <th class="num">Importe</th>
            <th class="acciones"><span class="cdk-visually-hidden">Acciones</span></th>
          </tr>
        </thead>
        <tbody>
          @for (e of encargos(); track e.id) {
            <tr class="clicable" [class.cancelado]="e.estado === 'CANCELADO'"
                (click)="abrir(e)" (keydown.enter)="abrir(e)" tabindex="0">
              <td><span class="referencia">{{ e.referencia }}</span></td>
              <td class="cifra nowrap">{{ e.fecha | fecha }}</td>
              @if (mostrarCliente()) {
                <td class="nowrap" (click)="$event.stopPropagation()">
                  <a [routerLink]="['/clientes', e.clienteId]">{{ e.clienteNombre }}</a>
                </td>
              }
              <td>
                <span class="tipo" [class.regalo]="e.tipo === 'REGALO'">
                  <mat-icon>{{ e.tipo === 'REGALO' ? 'redeem' : 'sell' }}</mat-icon>{{ nombreTipo[e.tipo] }}
                </span>
              </td>
              <td class="articulos">{{ articulos(e) }}</td>
              <td><app-estado-chip [estado]="e.estado" /></td>
              <td class="num">
                @if (e.precioAjustado) {
                  <mat-icon class="ajustado" matTooltip="Precio fijado a mano">edit_note</mat-icon>
                }
                <strong>{{ e.precioFinal | euros }}</strong>
              </td>
              <td class="acciones" (click)="$event.stopPropagation()" (keydown.enter)="$event.stopPropagation()">
                @if (siguiente(e); as sig) {
                  <button mat-stroked-button class="avanzar" (click)="avanzar(e)"
                          [attr.aria-label]="'Pasar ' + e.referencia + ' a ' + nombreEstado[sig]">
                    {{ nombreEstado[sig] }}
                    <mat-icon iconPositionEnd>arrow_forward</mat-icon>
                  </button>
                }
                <button mat-icon-button [matMenuTriggerFor]="menu" [attr.aria-label]="'Más acciones de ' + e.referencia">
                  <mat-icon>more_vert</mat-icon>
                </button>
                <mat-menu #menu="matMenu" xPosition="before">
                  <a mat-menu-item [routerLink]="['/encargos', e.id]"><mat-icon>edit</mat-icon> Abrir</a>
                  @if (e.tipo === 'VENTA') {
                    <a mat-menu-item [href]="url(e, 'presupuesto')" target="_blank" rel="noopener">
                      <mat-icon>request_quote</mat-icon> Presupuesto (PDF)
                    </a>
                  }
                  <a mat-menu-item [href]="url(e, 'albaran')" target="_blank" rel="noopener">
                    <mat-icon>receipt_long</mat-icon> Albarán (PDF)
                  </a>
                  <a mat-menu-item [href]="url(e, 'etiqueta')" target="_blank" rel="noopener">
                    <mat-icon>label</mat-icon> Etiqueta (PDF)
                  </a>
                  @if (e.estado !== 'CANCELADO') {
                    <button mat-menu-item (click)="cancelar(e)"><mat-icon>block</mat-icon> Cancelar encargo</button>
                  }
                  @if (puedeEliminar(e)) {
                    <button mat-menu-item (click)="eliminar(e)"><mat-icon>delete</mat-icon> Eliminar</button>
                  }
                </mat-menu>
              </td>
            </tr>
          }
        </tbody>
        @if (encargos().length > 1) {
          <tfoot>
            <tr>
              <td [attr.colspan]="mostrarCliente() ? 6 : 5">Total, {{ encargos().length }} encargos</td>
              <td class="num">{{ total() | euros }}</td>
              <td></td>
            </tr>
          </tfoot>
        }
      </table>
    </div>
  `,
  styles: `
    .nowrap { white-space: nowrap; }
    .tipo {
      display: inline-flex; align-items: center; gap: 4px; white-space: nowrap; color: var(--tinta-suave);
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }
    .tipo.regalo { color: #7a4a8c; }
    .tabla th, .tabla td { padding-left: 10px; padding-right: 10px; }
    .articulos { max-width: 22ch; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .cancelado td:not(.acciones) { color: #8b939f; }
    .ajustado { font-size: 18px; width: 18px; height: 18px; vertical-align: -4px; margin-right: 4px; color: var(--aviso); }
    .avanzar { margin-right: 2px; height: 32px; padding: 0 10px; font-size: 0.8125rem; }
  `,
})
export class TablaEncargos {
  private readonly router = inject(Router);
  private readonly acciones = inject(AccionesEncargo);
  private readonly avisos = inject(Avisos);
  private readonly api = inject(Api);

  readonly encargos = input.required<EncargoResumen[]>();
  readonly mostrarCliente = input(true);
  /** Se emite tras cambiar un estado o eliminar, para que la página recargue. */
  readonly cambiado = output<void>();

  protected readonly nombreEstado = NOMBRE_ESTADO;
  protected readonly nombreTipo = NOMBRE_TIPO;
  protected readonly total = computed(() =>
    this.encargos().filter((e) => e.estado !== 'CANCELADO').reduce((s, e) => s + e.precioFinal, 0),
  );

  protected articulos(e: EncargoResumen): string {
    return e.articulos
      .map((a) => (a.cantidad > 1 ? `${a.modelo} ×${a.cantidad}` : a.modelo) + (a.obsequio ? ' (obsequio)' : ''))
      .join(', ');
  }

  protected url(e: EncargoResumen, tipo: TipoDocumento): string {
    return this.api.urlDocumento(e.id, tipo);
  }

  protected siguiente(e: EncargoResumen) {
    return siguienteEstado(e.estado, e.tipo);
  }

  protected puedeEliminar(e: EncargoResumen): boolean {
    return sePuedeEliminar(e.estado);
  }

  protected abrir(e: EncargoResumen): void {
    this.router.navigate(['/encargos', e.id]);
  }

  protected avanzar(e: EncargoResumen): void {
    const sig = this.siguiente(e);
    if (sig) this.acciones.cambiarEstado(e, sig).subscribe((ok) => ok && this.cambiado.emit());
  }

  protected cancelar(e: EncargoResumen): void {
    this.acciones.cancelar(e).subscribe((ok) => ok && this.cambiado.emit());
  }

  protected eliminar(e: EncargoResumen): void {
    this.acciones.eliminar(e).subscribe({
      next: (ok) => ok && this.cambiado.emit(),
      error: (err) => this.avisos.error(err),
    });
  }
}
