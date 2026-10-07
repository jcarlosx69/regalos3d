import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Api } from '../../core/api';
import { AjustesDocumentos, Tarifas } from '../../core/modelos-api';
import { Avisos } from '../../shared/avisos';

const obligatorio = (min: number, max: number) => [Validators.required, Validators.min(min), Validators.max(max)];

@Component({
  selector: 'app-ajustes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="pagina">
      <header class="pagina-cabecera">
        <div>
          <h1>Ajustes</h1>
          <p>
            Tarifas por defecto del taller, que se copian en cada encargo nuevo (los existentes conservan las suyas),
            y datos de los documentos PDF.
          </p>
        </div>
      </header>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" />
      } @else {
        <form [formGroup]="form" (ngSubmit)="guardar()" class="ajustes">
          <section class="grupo" aria-labelledby="t-material">
            <div class="explicacion">
              <h2 id="t-material">Material</h2>
              <p>Precio de una bobina de 1 kg. Es el que se propone en cada línea de material y se puede cambiar en cada una.</p>
            </div>
            <div class="campos">
              <mat-form-field appearance="outline">
                <mat-label>Bobina de 1 kg</mat-label>
                <input matInput type="number" min="0" step="0.5" formControlName="precioKg" />
                <span matTextSuffix>€/kg</span>
              </mat-form-field>
            </div>
          </section>

          <section class="grupo" aria-labelledby="t-energia">
            <div class="explicacion">
              <h2 id="t-energia">Energía</h2>
              <p>Consumo medio de la impresora durante la impresión (mídelo con un enchufe medidor) y precio de la luz.</p>
            </div>
            <div class="campos">
              <mat-form-field appearance="outline">
                <mat-label>Potencia media</mat-label>
                <input matInput type="number" min="0" step="1" formControlName="potenciaW" />
                <span matTextSuffix>W</span>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Precio de la luz</mat-label>
                <input matInput type="number" min="0" step="0.001" formControlName="precioKwh" />
                <span matTextSuffix>€/kWh</span>
              </mat-form-field>
            </div>
          </section>

          <section class="grupo" aria-labelledby="t-taller">
            <div class="explicacion">
              <h2 id="t-taller">Taller</h2>
              <p>
                Mano de obra: diseño, preparación y postprocesado, por hora. Amortización: desgaste de la impresora
                y repuestos (boquillas, placas, correas) por hora de impresión; entra siempre en el coste.
              </p>
            </div>
            <div class="campos">
              <mat-form-field appearance="outline">
                <mat-label>Mano de obra</mat-label>
                <input matInput type="number" min="0" step="0.5" formControlName="manoObraHora" />
                <span matTextSuffix>€/h</span>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Amortización</mat-label>
                <input matInput type="number" min="0" step="0.05" formControlName="amortizacionHora" />
                <span matTextSuffix>€/h</span>
              </mat-form-field>
            </div>
          </section>

          <section class="grupo" aria-labelledby="t-venta">
            <div class="explicacion">
              <h2 id="t-venta">Venta</h2>
              <p>Porcentaje que se suma al coste más la mano de obra en los encargos que aplican margen.</p>
            </div>
            <div class="campos">
              <mat-form-field appearance="outline">
                <mat-label>Margen de beneficio</mat-label>
                <input matInput type="number" min="0" step="1" formControlName="margenPct" />
                <span matTextSuffix>%</span>
              </mat-form-field>
            </div>
          </section>

          <section class="grupo" aria-labelledby="t-documentos" [formGroup]="docs">
            <div class="explicacion">
              <h2 id="t-documentos">Documentos</h2>
              <p>
                Validez que se indica en los presupuestos y texto que aparece al pie del presupuesto y del albarán:
                condiciones, plazo de entrega, forma de pago o un agradecimiento. Las notas internas de los encargos
                nunca salen en los documentos.
              </p>
            </div>
            <div class="campos">
              <mat-form-field appearance="outline">
                <mat-label>Validez del presupuesto</mat-label>
                <input matInput type="number" min="1" max="365" step="1" formControlName="validezPresupuestoDias" />
                <span matTextSuffix>días</span>
                @if (docs.controls.validezPresupuestoDias.invalid) { <mat-error>Entre 1 y 365 días</mat-error> }
              </mat-form-field>
              <mat-form-field appearance="outline" class="ancho-completo">
                <mat-label>Texto al pie</mat-label>
                <textarea matInput formControlName="textoPie" rows="3" maxlength="500"
                          placeholder="Plazo de entrega orientativo: una semana desde la aceptación."></textarea>
                <mat-hint align="end">{{ docs.controls.textoPie.value.length }} / 500</mat-hint>
              </mat-form-field>
            </div>
          </section>

          <div class="pie">
            <button mat-flat-button type="submit" [disabled]="guardando() || (form.pristine && docs.pristine)">
              {{ guardando() ? 'Guardando…' : 'Guardar ajustes' }}
            </button>
            @if (form.dirty || docs.dirty) {
              <button mat-button type="button" (click)="descartar()">Descartar cambios</button>
            }
          </div>
        </form>
      }
    </div>
  `,
  styles: `
    .ajustes { max-width: 880px; display: grid; gap: 0; }
    .grupo {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1.1fr);
      gap: 24px;
      padding: 20px 0;
      border-bottom: 1px solid var(--linea);
    }
    .grupo:first-child { padding-top: 4px; }
    .explicacion p { margin: 4px 0 0; color: var(--tinta-suave); font-size: 0.9rem; max-width: 46ch; }
    .campos { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; align-content: start; }
    .ancho-completo { grid-column: 1 / -1; }
    .pie { display: flex; gap: 8px; padding-top: 20px; }
    @media (max-width: 720px) {
      .grupo { grid-template-columns: 1fr; gap: 10px; }
    }
  `,
})
export class Ajustes {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  private original: Tarifas | null = null;
  private originalDocs: AjustesDocumentos | null = null;

  protected readonly form = new FormGroup({
    precioKg: new FormControl<number | null>(null, obligatorio(0.01, 9999)),
    potenciaW: new FormControl<number | null>(null, obligatorio(0.1, 99999)),
    precioKwh: new FormControl<number | null>(null, obligatorio(0.0001, 99)),
    manoObraHora: new FormControl<number | null>(null, obligatorio(0, 9999)),
    amortizacionHora: new FormControl<number | null>(null, obligatorio(0, 9999)),
    margenPct: new FormControl<number | null>(null, obligatorio(0, 999)),
  });

  protected readonly docs = new FormGroup({
    validezPresupuestoDias: new FormControl<number | null>(30, [...obligatorio(1, 365), Validators.pattern(/^\d+$/)]),
    textoPie: new FormControl('', { nonNullable: true, validators: Validators.maxLength(500) }),
  });

  constructor() {
    forkJoin([this.api.ajustes(), this.api.ajustesDocumentos()]).subscribe({
      next: ([t, d]) => {
        this.original = t;
        this.form.reset(t);
        this.rellenarDocs(d);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.avisos.error(e);
      },
    });
  }

  protected guardar(): void {
    if (this.form.invalid || this.docs.invalid) {
      this.form.markAllAsTouched();
      this.docs.markAllAsTouched();
      this.avisos.error(new Error('Revisa los valores marcados en rojo.'));
      return;
    }
    // Solo se envía lo que ha cambiado
    const tarifas$: Observable<Tarifas | null> = this.form.dirty
      ? this.api.guardarAjustes(this.form.getRawValue() as Tarifas)
      : of(null);
    const v = this.docs.getRawValue();
    const docs$: Observable<AjustesDocumentos | null> = this.docs.dirty
      ? this.api.guardarAjustesDocumentos({ validezPresupuestoDias: v.validezPresupuestoDias as number, textoPie: v.textoPie.trim() || null })
      : of(null);
    this.guardando.set(true);
    forkJoin([tarifas$, docs$]).subscribe({
      next: ([t, d]) => {
        if (t) {
          this.original = t;
          this.form.reset(t);
        }
        if (d) this.rellenarDocs(d);
        this.guardando.set(false);
        this.avisos.hecho('Ajustes guardados');
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }

  protected descartar(): void {
    if (this.original) this.form.reset(this.original);
    if (this.originalDocs) this.rellenarDocs(this.originalDocs);
  }

  private rellenarDocs(d: AjustesDocumentos): void {
    this.originalDocs = d;
    this.docs.reset({ validezPresupuestoDias: d.validezPresupuestoDias, textoPie: d.textoPie ?? '' });
  }
}
