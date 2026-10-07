import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, Observable, map, switchMap } from 'rxjs';
import { Api } from '../../core/api';
import { NOMBRE_ESTADO } from '../../core/estados';
import { EstadoEncargo } from '../../core/modelos-api';
import { Avisos } from '../../shared/avisos';
import { confirmar } from '../../shared/confirmar';

interface EncargoBasico {
  id: number;
  referencia: string;
}

/** Cambios de estado y borrado desde listas y fichas, con confirmación y aviso. Emite true si hubo cambio. */
@Injectable({ providedIn: 'root' })
export class AccionesEncargo {
  private readonly api = inject(Api);
  private readonly dialog = inject(MatDialog);
  private readonly avisos = inject(Avisos);

  cambiarEstado(e: EncargoBasico, estado: EstadoEncargo): Observable<boolean> {
    return new Observable<boolean>((sub) => {
      this.api.cambiarEstado(e.id, estado).subscribe({
        next: () => {
          this.avisos.hecho(`${e.referencia}: ${NOMBRE_ESTADO[estado]}`);
          sub.next(true);
          sub.complete();
        },
        error: (err) => {
          this.avisos.error(err);
          sub.next(false);
          sub.complete();
        },
      });
    });
  }

  cancelar(e: EncargoBasico): Observable<boolean> {
    return confirmar(this.dialog, {
      titulo: `¿Cancelar ${e.referencia}?`,
      texto: 'El encargo se conserva como cancelado y deja de contar en los informes. Puedes reactivarlo cambiando su estado.',
      aceptar: 'Cancelar encargo',
      peligroso: true,
    }).pipe(switchMap((ok) => (ok ? this.cambiarEstado(e, 'CANCELADO') : EMPTY)));
  }

  eliminar(e: EncargoBasico): Observable<boolean> {
    return confirmar(this.dialog, {
      titulo: `¿Eliminar ${e.referencia}?`,
      texto: 'Se elimina definitivamente con todos sus artículos. Solo es posible en presupuestos y encargos cancelados.',
      aceptar: 'Eliminar',
      peligroso: true,
    }).pipe(
      switchMap((ok) => (ok ? this.api.borrarEncargo(e.id).pipe(map(() => true)) : EMPTY)),
      map((hecho) => {
        if (hecho) this.avisos.hecho(`${e.referencia} eliminado`);
        return hecho;
      }),
    );
  }
}
