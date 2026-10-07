import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Api } from '../../core/api';
import { Modelo } from '../../core/modelos-api';
import { Avisos } from '../../shared/avisos';
import { confirmar } from '../../shared/confirmar';
import { abrirModeloDialog } from './modelo-dialog';

@Component({
  selector: 'app-modelos-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatProgressBarModule, MatTooltipModule],
  template: `
    <div class="pagina">
      <header class="pagina-cabecera">
        <div>
          <h1>Modelos</h1>
          <p>Catálogo de modelos enlazados con la biblioteca de Manyfold.</p>
        </div>
        <button mat-flat-button (click)="nuevo()"><mat-icon>add</mat-icon> Nuevo modelo</button>
      </header>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (modelos().length) {
        <div class="herramientas">
          <mat-form-field appearance="outline" class="buscador">
            <mat-icon matPrefix>search</mat-icon>
            <mat-label>Buscar por nombre</mat-label>
            <input matInput [value]="filtro()" (input)="filtro.set($any($event.target).value)" />
          </mat-form-field>
          <span class="recuento">{{ filtrados().length }} de {{ modelos().length }} modelos</span>
        </div>

        <div class="tabla-scroll">
          <table class="tabla">
            <thead>
              <tr>
                <th>Nombre</th><th>ID en Manyfold</th><th class="num">Peso estimado</th>
                <th class="acciones"><span class="cdk-visually-hidden">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (m of filtrados(); track m.id) {
                <tr>
                  <td><strong>{{ m.nombre }}</strong></td>
                  <td>
                    <a [href]="m.manyfoldUrl" target="_blank" rel="noopener" class="enlace-manyfold">
                      {{ m.manyfoldModelId }} <mat-icon inline>open_in_new</mat-icon>
                    </a>
                  </td>
                  <td class="num">{{ m.gramosEstimados != null ? m.gramosEstimados + ' g' : '—' }}</td>
                  <td class="acciones">
                    <button mat-icon-button (click)="editar(m)" matTooltip="Editar" [attr.aria-label]="'Editar ' + m.nombre">
                      <mat-icon>edit</mat-icon>
                    </button>
                    <button mat-icon-button (click)="borrar(m)" matTooltip="Eliminar" [attr.aria-label]="'Eliminar ' + m.nombre">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="4" class="suave">Ningún modelo coincide con «{{ filtro() }}».</td></tr>
              }
            </tbody>
          </table>
        </div>
      } @else {
        <div class="vacio">
          <h2>No hay modelos registrados</h2>
          <p>
            Sube el objeto a la biblioteca de Manyfold y registra aquí su identificador para poder
            asociarlo a encargos.
          </p>
          <button mat-flat-button (click)="nuevo()"><mat-icon>add</mat-icon> Nuevo modelo</button>
        </div>
      }
    </div>
  `,
  styles: `
    .enlace-manyfold { display: inline-flex; align-items: center; gap: 4px; font-variant-numeric: tabular-nums; }
  `,
})
export class ModelosLista {
  private readonly api = inject(Api);
  private readonly dialog = inject(MatDialog);
  private readonly avisos = inject(Avisos);

  protected readonly modelos = signal<Modelo[]>([]);
  protected readonly cargando = signal(true);
  protected readonly filtro = signal('');
  protected readonly filtrados = computed(() => {
    const f = this.filtro().trim().toLowerCase();
    return f ? this.modelos().filter((m) => m.nombre.toLowerCase().includes(f)) : this.modelos();
  });

  constructor() {
    this.cargar();
  }

  private cargar(): void {
    this.api.modelos().subscribe({
      next: (ms) => {
        this.modelos.set(ms);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.avisos.error(e);
      },
    });
  }

  protected nuevo(): void {
    abrirModeloDialog(this.dialog).subscribe((m) => {
      if (m) {
        this.avisos.hecho('Modelo registrado');
        this.cargar();
      }
    });
  }

  protected editar(m: Modelo): void {
    abrirModeloDialog(this.dialog, m).subscribe((g) => {
      if (g) {
        this.avisos.hecho('Cambios guardados');
        this.cargar();
      }
    });
  }

  protected borrar(m: Modelo): void {
    confirmar(this.dialog, {
      titulo: `¿Eliminar «${m.nombre}»?`,
      texto: 'Se elimina solo el registro en esta aplicación; los ficheros de Manyfold no se tocan. Un modelo que figura en encargos no se puede eliminar.',
      aceptar: 'Eliminar',
      peligroso: true,
    }).subscribe((ok) => {
      if (!ok) return;
      this.api.borrarModelo(m.id).subscribe({
        next: () => {
          this.avisos.hecho('Modelo eliminado');
          this.cargar();
        },
        error: (e) => this.avisos.error(e),
      });
    });
  }
}
