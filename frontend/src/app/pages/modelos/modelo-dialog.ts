import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';
import { Api } from '../../core/api';
import { Modelo } from '../../core/modelos-api';
import { mensajeError } from '../../shared/avisos';

@Component({
  selector: 'app-modelo-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ modelo ? 'Editar modelo' : 'Nuevo modelo' }}</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content>
        <p class="suave ayuda">
          Los ficheros (STL, 3MF, FreeCAD) y las fotos se gestionan en Manyfold; aquí se registra la referencia.
        </p>

        <mat-form-field appearance="outline">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" maxlength="150" cdkFocusInitial />
          @if (form.controls.nombre.hasError('required')) { <mat-error>Escribe el nombre.</mat-error> }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>ID en Manyfold</mat-label>
          <input matInput formControlName="manyfoldModelId" maxlength="64" (paste)="pegarUrl($event)" />
          <mat-hint>Copia el identificador de la dirección del modelo, o pega la dirección entera.</mat-hint>
          @if (form.controls.manyfoldModelId.hasError('required')) { <mat-error>Falta el ID de Manyfold.</mat-error> }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Peso estimado</mat-label>
          <input matInput type="number" min="0" step="0.1" [formControl]="gramos" />
          <span matTextSuffix>g</span>
          <mat-hint>Opcional. Se propone como gramos de la primera bobina del artículo.</mat-hint>
        </mat-form-field>

        @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">
          {{ modelo ? 'Guardar cambios' : 'Guardar modelo' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    mat-dialog-content { display: grid; gap: 10px; min-width: min(440px, 80vw); }
    .ayuda { margin: 0 0 12px; max-width: 48ch; }
    .error { color: var(--peligro); margin: 4px 0 0; }
  `,
})
export class ModeloDialog {
  private readonly api = inject(Api);
  private readonly ref = inject(MatDialogRef<ModeloDialog, Modelo>);
  protected readonly modelo = inject<Modelo | null>(MAT_DIALOG_DATA);

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: [this.modelo?.nombre ?? '', [Validators.required, Validators.maxLength(150)]],
    manyfoldModelId: [this.modelo?.manyfoldModelId ?? '', [Validators.required, Validators.maxLength(64)]],
  });
  protected readonly gramos = new FormControl<number | null>(this.modelo?.gramosEstimados ?? null, Validators.min(0));

  /** Si se pega la URL completa del modelo (…/models/abc123), se queda solo con el identificador. */
  protected pegarUrl(evento: ClipboardEvent): void {
    const texto = evento.clipboardData?.getData('text')?.trim() ?? '';
    const m = /\/models\/([^/?#]+)/.exec(texto);
    if (m) {
      evento.preventDefault();
      this.form.controls.manyfoldModelId.setValue(decodeURIComponent(m[1]));
    }
  }

  protected guardar(): void {
    if (this.form.invalid || this.gramos.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const cuerpo = {
      nombre: v.nombre.trim(),
      manyfoldModelId: v.manyfoldModelId.trim(),
      gramosEstimados: this.gramos.value,
    };
    const peticion = this.modelo ? this.api.actualizarModelo(this.modelo.id, cuerpo) : this.api.crearModelo(cuerpo);
    this.guardando.set(true);
    this.error.set(null);
    peticion.subscribe({
      next: (m) => this.ref.close(m),
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }
}

export function abrirModeloDialog(dialog: MatDialog, modelo: Modelo | null = null): Observable<Modelo | undefined> {
  return dialog.open<ModeloDialog, Modelo | null, Modelo>(ModeloDialog, { data: modelo }).afterClosed();
}
