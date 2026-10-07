import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';
import { Api } from '../../core/api';
import { Cliente } from '../../core/modelos-api';
import { mensajeError } from '../../shared/avisos';

/** Acepta los formatos habituales; el backend normaliza a +34XXXXXXXXX. */
const MOVIL = /^\s*(\+34|0034|34)?[\s.\-()]*[67]([\s.\-()]*\d){8}\s*$/;

@Component({
  selector: 'app-cliente-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ cliente ? 'Editar cliente' : 'Nuevo cliente' }}</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content>
        <div class="fila">
          <mat-form-field appearance="outline">
            <mat-label>Nombre</mat-label>
            <input matInput formControlName="nombre" maxlength="100" cdkFocusInitial />
            @if (form.controls.nombre.hasError('required')) { <mat-error>Escribe el nombre.</mat-error> }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Móvil</mat-label>
            <input matInput formControlName="telefono" inputmode="tel" placeholder="600 12 34 56" />
            <mat-hint>Identifica al cliente: no puede repetirse.</mat-hint>
            @if (form.controls.telefono.hasError('required')) {
              <mat-error>Escribe el móvil.</mat-error>
            } @else if (form.controls.telefono.hasError('pattern')) {
              <mat-error>Móvil español de 9 cifras que empiece por 6 o 7.</mat-error>
            }
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline">
          <mat-label>Email (opcional)</mat-label>
          <input matInput type="email" formControlName="email" maxlength="150" autocomplete="off" />
          @if (form.controls.email.hasError('email')) { <mat-error>No parece un email válido.</mat-error> }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Dirección de entrega (opcional)</mat-label>
          <textarea matInput formControlName="direccion" rows="2" maxlength="300"></textarea>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Notas</mat-label>
          <textarea matInput formControlName="notas" rows="2" maxlength="500"
                    placeholder="Preferencias, colores, observaciones"></textarea>
        </mat-form-field>

        @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="guardando()">
          {{ cliente ? 'Guardar cambios' : 'Guardar cliente' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    mat-dialog-content { display: grid; gap: 6px; width: min(560px, 86vw); padding-top: 8px !important; }
    .fila { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .error { color: var(--peligro); margin: 4px 0 0; }
    @media (max-width: 600px) { .fila { grid-template-columns: 1fr; } }
  `,
})
export class ClienteDialog {
  private readonly api = inject(Api);
  private readonly ref = inject(MatDialogRef<ClienteDialog, Cliente>);
  protected readonly cliente = inject<Cliente | null>(MAT_DIALOG_DATA);

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: [this.cliente?.nombre ?? '', [Validators.required, Validators.maxLength(100)]],
    telefono: [this.cliente?.telefono ?? '', [Validators.required, Validators.pattern(MOVIL)]],
    email: [this.cliente?.email ?? '', [Validators.email, Validators.maxLength(150)]],
    direccion: [this.cliente?.direccion ?? ''],
    notas: [this.cliente?.notas ?? ''],
  });

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const cuerpo = {
      nombre: v.nombre.trim(),
      telefono: v.telefono,
      email: v.email.trim() || null,
      direccion: v.direccion.trim() || null,
      notas: v.notas.trim() || null,
    };
    const peticion = this.cliente ? this.api.actualizarCliente(this.cliente.id, cuerpo) : this.api.crearCliente(cuerpo);
    this.guardando.set(true);
    this.error.set(null);
    peticion.subscribe({
      next: (c) => this.ref.close(c),
      error: (e) => {
        this.guardando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }
}

/** Abre el diálogo; emite el cliente guardado o undefined si se cancela. */
export function abrirClienteDialog(dialog: MatDialog, cliente: Cliente | null = null): Observable<Cliente | undefined> {
  return dialog.open<ClienteDialog, Cliente | null, Cliente>(ClienteDialog, { data: cliente }).afterClosed();
}
