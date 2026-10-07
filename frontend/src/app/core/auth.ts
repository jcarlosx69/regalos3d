import { HttpClient, HttpErrorResponse, HttpInterceptorFn, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Observable, catchError, map, of, tap, throwError } from 'rxjs';

/**
 * Sesión con el backend (Spring Security). El login crea una cookie de sesión HttpOnly;
 * aquí solo se recuerda el nombre del usuario para la interfaz.
 */
@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _usuario = signal<string | null>(null);
  readonly usuario = this._usuario.asReadonly();
  readonly conectado = computed(() => this._usuario() !== null);

  /** Pregunta al backend si hay sesión. Además obtiene la cookie CSRF para el primer POST. */
  comprobar(): Observable<boolean> {
    return this.http.get<{ usuario: string }>('/api/auth/yo').pipe(
      tap((r) => this._usuario.set(r.usuario)),
      map(() => true),
      catchError(() => {
        this._usuario.set(null);
        return of(false);
      }),
    );
  }

  /** Spring Security espera un formulario clásico con los campos username y password. */
  entrar(usuario: string, password: string): Observable<boolean> {
    const cuerpo = new HttpParams().set('username', usuario).set('password', password);
    return this.http
      .post('/api/auth/login', cuerpo.toString(), {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      })
      .pipe(
        map(() => {
          this._usuario.set(usuario);
          return true;
        }),
        catchError((e: HttpErrorResponse) => (e.status === 401 ? of(false) : throwError(() => e))),
      );
  }

  salir(): void {
    this.http.post('/api/auth/logout', null).subscribe({
      complete: () => this.cerrarSesionLocal(),
      error: () => this.cerrarSesionLocal(),
    });
  }

  /** La sesión ha caducado o se cerró: se olvida el usuario y se vuelve al login. */
  cerrarSesionLocal(volverA?: string): void {
    this._usuario.set(null);
    const queryParams = volverA && volverA !== '/login' ? { volver: volverA } : undefined;
    this.router.navigate(['/login'], { queryParams });
  }
}

/** Deja pasar a las pantallas de la app solo con sesión iniciada. */
export const requiereSesion: CanActivateFn = (_route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);
  if (auth.conectado()) return true;
  return auth.comprobar().pipe(
    map((ok) => ok || router.createUrlTree(['/login'], { queryParams: { volver: state.url } })),
  );
};

/** Si una llamada a la API responde 401 (sesión caducada), lleva al login. */
export const sesionCaducadaInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const router = inject(Router);
  return next(req).pipe(
    catchError((e: unknown) => {
      const esAuth = req.url.startsWith('/api/auth/');
      if (e instanceof HttpErrorResponse && e.status === 401 && !esAuth && auth.conectado()) {
        auth.cerrarSesionLocal(router.url);
      }
      return throwError(() => e);
    }),
  );
};
