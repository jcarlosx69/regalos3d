import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Problema } from '../core/modelos-api';

/** Convierte un error HTTP en una frase útil, usando el ProblemDetail del backend cuando existe. */
export function mensajeError(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) return 'No hay conexión con el servidor. Comprueba que la app está en marcha.';
    const p = e.error as Problema | null;
    if (p?.campos) {
      const primero = Object.entries(p.campos)[0];
      if (primero) return `${primero[0]}: ${primero[1]}`;
    }
    if (p?.detail) return p.detail;
    if (e.status === 403) return 'El servidor rechazó la operación. Recarga la página e inténtalo de nuevo.';
    return `Error ${e.status} del servidor.`;
  }
  if (e instanceof Error && e.message) return e.message;
  return 'Ha ocurrido un error inesperado.';
}

/** Mensajes breves en la parte inferior de la pantalla. */
@Injectable({ providedIn: 'root' })
export class Avisos {
  private readonly snack = inject(MatSnackBar);

  hecho(texto: string): void {
    this.snack.open(texto, undefined, { duration: 3000 });
  }

  error(e: unknown): void {
    this.snack.open(mensajeError(e), 'Cerrar', { duration: 8000, panelClass: 'snack-error' });
  }
}
