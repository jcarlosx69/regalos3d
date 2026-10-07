import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Api } from '../../core/api';
import { ImporteAnio, ImporteCliente, ImporteOcasion, ModeloEncargado, Resumen } from '../../core/modelos-api';
import { Avisos } from '../../shared/avisos';
import { EurosPipe } from '../../shared/formato';

@Component({
  selector: 'app-informes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonToggleModule, MatIconModule, MatProgressBarModule, EurosPipe],
  template: `
    <div class="pagina">
      <header class="pagina-cabecera">
        <div>
          <h1>Informes</h1>
          <p>Facturación, beneficio y coste de regalos. Cuentan los encargos entregados o cobrados.</p>
        </div>
        @if (anios().length) {
          <mat-button-toggle-group [value]="anio()" (change)="anio.set($event.value)" aria-label="Periodo" hideSingleSelectionIndicator>
            <mat-button-toggle [value]="null">Todo</mat-button-toggle>
            @for (a of anios(); track a.anio) {
              <mat-button-toggle [value]="a.anio">{{ a.anio }}</mat-button-toggle>
            }
          </mat-button-toggle-group>
        }
      </header>

      @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }

      @if (resumen(); as r) {
        <dl class="indicadores principal" aria-label="Ventas del periodo">
          <div>
            <dt>Facturado</dt>
            <dd class="cifra">{{ r.facturado | euros }}</dd>
            <span class="pie">{{ r.ventas }} {{ r.ventas === 1 ? 'venta' : 'ventas' }}</span>
          </div>
          <div>
            <dt>Cobrado</dt>
            <dd class="cifra">{{ r.cobrado | euros }}</dd>
          </div>
          <div [class.alerta]="r.pendienteCobro > 0">
            <dt>Pendiente de cobro</dt>
            <dd class="cifra">{{ r.pendienteCobro | euros }}</dd>
          </div>
          <div>
            <dt>Beneficio</dt>
            <dd class="cifra" [class.negativo]="r.beneficio < 0">{{ r.beneficio | euros }}</dd>
            @if (r.facturado > 0) {
              <span class="pie">{{ porcentaje(r.beneficio, r.facturado) }} % sobre lo facturado</span>
            }
          </div>
        </dl>

        <dl class="indicadores secundario" aria-label="Regalos y trabajo en curso">
          <div>
            <dt>Coste en regalos</dt>
            <dd class="cifra">{{ r.costeRegalos | euros }}</dd>
            <span class="pie">{{ r.regalos }} {{ r.regalos === 1 ? 'regalo entregado' : 'regalos entregados' }}</span>
          </div>
          <div>
            <dt>En curso</dt>
            <dd class="cifra">{{ r.importeEnCurso | euros }}</dd>
            <span class="pie">{{ r.enCurso }} {{ r.enCurso === 1 ? 'encargo' : 'encargos' }} entre aceptado y terminado</span>
          </div>
          <div>
            <dt>Presupuestos abiertos</dt>
            <dd class="cifra">{{ r.importePresupuestos | euros }}</dd>
            <span class="pie">{{ r.presupuestos }} sin aceptar</span>
          </div>
        </dl>

        @if (!porCliente().length && !modelos().length) {
          <div class="vacio">
            <h2>Sin encargos entregados {{ anio() ? 'en ' + anio() : 'todavía' }}</h2>
            <p>Los informes se llenan a medida que los encargos llegan a Entregado o Cobrado.</p>
            <a routerLink="/encargos">Ir a encargos</a>
          </div>
        } @else {
          <div class="dos-columnas">
            <section aria-labelledby="t-clientes">
              <h2 id="t-clientes">Por cliente</h2>
              <table class="tabla">
                <thead>
                  <tr><th>Cliente</th><th class="num">Encargos</th><th class="num">Facturado</th><th class="num">Coste regalos</th></tr>
                </thead>
                <tbody>
                  @for (c of porCliente(); track c.clienteId) {
                    <tr>
                      <td>
                        <a [routerLink]="['/clientes', c.clienteId]">{{ c.nombre }}</a>
                        <span class="barra" [style.width.%]="(c.facturado / maxCliente()) * 100"></span>
                      </td>
                      <td class="num">{{ c.encargos }}</td>
                      <td class="num">{{ c.facturado | euros }}</td>
                      <td class="num">{{ c.costeRegalos | euros }}</td>
                    </tr>
                  } @empty {
                    <tr><td colspan="4" class="suave">Sin encargos entregados en este periodo.</td></tr>
                  }
                </tbody>
              </table>
            </section>

            <section aria-labelledby="t-modelos">
              <h2 id="t-modelos">Modelos más encargados</h2>
              <table class="tabla">
                <thead><tr><th>Modelo</th><th class="num">Unidades</th><th class="num">Encargos</th><th class="num">Clientes</th></tr></thead>
                <tbody>
                  @for (m of modelos(); track m.modeloId) {
                    <tr>
                      <td>{{ m.nombre }}</td>
                      <td class="num">{{ m.unidades }}</td>
                      <td class="num">{{ m.encargos }}</td>
                      <td class="num">{{ m.clientes }}</td>
                    </tr>
                  }
                </tbody>
              </table>
              <p class="suave nota">Encargos aceptados o posteriores, de todos los años.</p>
            </section>
          </div>

          <div class="dos-columnas">
            <section aria-labelledby="t-anios">
              <h2 id="t-anios">Por año</h2>
              <table class="tabla">
                <thead>
                  <tr><th>Año</th><th class="num">Ventas</th><th class="num">Facturado</th><th class="num">Beneficio</th><th class="num">Coste regalos</th></tr>
                </thead>
                <tbody>
                  @for (a of anios(); track a.anio) {
                    <tr [class.actual]="a.anio === anio()">
                      <td class="cifra">{{ a.anio }}</td>
                      <td class="num">{{ a.ventas }}</td>
                      <td class="num">{{ a.facturado | euros }}</td>
                      <td class="num">{{ a.beneficio | euros }}</td>
                      <td class="num">{{ a.costeRegalos | euros }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </section>

            <section aria-labelledby="t-ocasion">
              <h2 id="t-ocasion">Por ocasión</h2>
              <table class="tabla">
                <thead><tr><th>Ocasión</th><th class="num">Encargos</th><th class="num">Importe</th></tr></thead>
                <tbody>
                  @for (o of porOcasion(); track o.ocasion) {
                    <tr>
                      <td>{{ o.ocasion }}</td>
                      <td class="num">{{ o.encargos }}</td>
                      <td class="num">{{ o.importe | euros }}</td>
                    </tr>
                  } @empty {
                    <tr><td colspan="3" class="suave">Sin encargos entregados en este periodo.</td></tr>
                  }
                </tbody>
              </table>
            </section>
          </div>
        }
      }
    </div>
  `,
  styles: `
    .indicadores {
      display: grid;
      margin: 0 0 16px;
      background: var(--superficie);
      border: 1px solid var(--linea);
      border-radius: var(--radio-panel);

      > div { padding: 14px 18px; border-left: 1px solid var(--linea); min-width: 0; }
      > div:first-child { border-left: none; }
      dt { font-size: 0.8125rem; color: var(--tinta-suave); }
      dd { margin: 4px 0 2px; font-weight: 650; font-stretch: 88%; letter-spacing: -0.01em; }
      .pie { font-size: 0.8125rem; color: var(--tinta-suave); }
      .negativo { color: var(--peligro); }
      .alerta dd { color: var(--aviso-tinta); }
    }
    .principal { grid-template-columns: repeat(4, minmax(0, 1fr)); }
    .principal dd { font-size: 1.75rem; }
    .secundario { grid-template-columns: repeat(3, minmax(0, 1fr)); margin-bottom: 28px; background: var(--superficie-2); }
    .secundario dd { font-size: 1.25rem; }
    .dos-columnas {
      display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 28px; margin-bottom: 28px;
    }
    section h2 { margin-bottom: 12px; }
    .barra {
      display: block; height: 4px; margin-top: 4px; border-radius: 2px;
      background: rgba(14, 90, 98, 0.45); min-width: 2px;
    }
    tr.actual td { background: var(--accion-suave); font-weight: 600; }
    .nota { margin: 8px 0 0; font-size: 0.875rem; }
    @media (max-width: 960px) {
      .dos-columnas { grid-template-columns: 1fr; }
      .principal { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .principal > div:nth-child(3) { border-left: none; }
      .principal > div:nth-child(n + 3) { border-top: 1px solid var(--linea); }
      .secundario { grid-template-columns: 1fr; }
      .secundario > div { border-left: none; border-top: 1px solid var(--linea); }
      .secundario > div:first-child { border-top: none; }
    }
  `,
})
export class Informes {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  protected readonly anio = signal<number | null>(null);
  protected readonly cargando = signal(true);

  protected readonly resumen = signal<Resumen | null>(null);
  protected readonly porCliente = signal<ImporteCliente[]>([]);
  protected readonly porOcasion = signal<ImporteOcasion[]>([]);
  protected readonly anios = signal<ImporteAnio[]>([]);
  protected readonly modelos = signal<ModeloEncargado[]>([]);

  protected readonly maxCliente = computed(() => Math.max(0.01, ...this.porCliente().map((c) => c.facturado)));

  constructor() {
    forkJoin([this.api.porAnio(), this.api.modelosEncargados(10)]).subscribe({
      next: ([anios, modelos]) => {
        this.anios.set(anios);
        this.modelos.set(modelos);
      },
      error: (e) => this.avisos.error(e),
    });

    effect(() => {
      const anio = this.anio();
      this.cargando.set(true);
      forkJoin([this.api.resumen(anio), this.api.porCliente(anio), this.api.porOcasion(anio)]).subscribe({
        next: ([r, clientes, ocasiones]) => {
          this.resumen.set(r);
          this.porCliente.set(clientes);
          this.porOcasion.set(ocasiones);
          this.cargando.set(false);
        },
        error: (e) => {
          this.cargando.set(false);
          this.avisos.error(e);
        },
      });
    });
  }

  protected porcentaje(parte: number, total: number): number {
    return total ? Math.round((parte / total) * 100) : 0;
  }
}
