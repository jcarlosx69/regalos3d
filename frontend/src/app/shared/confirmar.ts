import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable } from 'rxjs';

export interface DatosConfirmar {
  titulo: string;
  texto: string;
  aceptar: string;
  peligroso?: boolean;
}

@Component({
  selector: 'app-confirmar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content><p>{{ datos.texto }}</p></mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button [class.peligro]="datos.peligroso" [mat-dialog-close]="true" cdkFocusInitial>
        {{ datos.aceptar }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    p { margin: 0; max-width: 52ch; }
    .peligro { --mat-button-filled-container-color: var(--peligro); }
  `,
})
export class Confirmar {
  protected readonly datos = inject<DatosConfirmar>(MAT_DIALOG_DATA);
}

/** Abre el diálogo y emite true solo si se acepta. */
export function confirmar(dialog: MatDialog, datos: DatosConfirmar): Observable<boolean | undefined> {
  return dialog.open<Confirmar, DatosConfirmar, boolean>(Confirmar, { data: datos, width: '440px' }).afterClosed();
}
