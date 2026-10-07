import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from '../core/auth';
import { Logo } from '../shared/logo';

/** Estructura común: barra lateral con la navegación y la zona de contenido. */
@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, MatButtonModule, Logo],
  template: `
    <div class="marco">
      <nav class="lateral" aria-label="Secciones">
        <a class="marca" routerLink="/encargos" aria-label="Regalos 3D, inicio">
          <app-logo />
          <span class="nombre">
            <strong>Regalos 3D</strong>
            <small>Gestión de impresiones</small>
          </span>
        </a>

        <a mat-flat-button class="nuevo" routerLink="/encargos/nuevo">
          <mat-icon>add</mat-icon> Nuevo encargo
        </a>

        <ul>
          @for (s of secciones; track s.ruta) {
            <li>
              <a [routerLink]="s.ruta" routerLinkActive="activa" [routerLinkActiveOptions]="{ exact: s.exacta }">
                <mat-icon>{{ s.icono }}</mat-icon>
                <span>{{ s.nombre }}</span>
              </a>
            </li>
          }
        </ul>

        <div class="pie">
          <div class="usuario">
            <span class="inicial" aria-hidden="true">{{ inicial() }}</span>
            <span class="quien">{{ auth.usuario() }}</span>
          </div>
          <button mat-icon-button class="salir" (click)="auth.salir()" aria-label="Cerrar sesión" title="Cerrar sesión">
            <mat-icon>logout</mat-icon>
          </button>
        </div>
      </nav>

      <main class="contenido">
        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    .marco {
      display: grid;
      grid-template-columns: 236px 1fr;
      min-height: 100vh;
      /* el fondo de la barra llega hasta abajo aunque la página sea larga */
      background: linear-gradient(to right, var(--grafito) 236px, transparent 236px);
    }
    .lateral {
      position: sticky;
      top: 0;
      height: 100vh;
      display: flex;
      flex-direction: column;
      gap: 22px;
      padding: 20px 14px;
      color: #c8cfda;
    }
    .marca {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 2px 8px 0;
      text-decoration: none;
      color: #ffffff;
    }
    .nombre { display: grid; line-height: 1.15; }
    .nombre strong { font-size: 1.05rem; font-weight: 700; font-stretch: 90%; letter-spacing: 0; }
    .nombre small { font-size: 0.75rem; color: #8d97a6; }
    .nuevo {
      justify-content: flex-start;
      --mat-button-filled-container-color: #1d7a83;
      --mat-button-filled-label-text-color: #fff;
    }
    ul { list-style: none; margin: 0; padding: 0; display: grid; gap: 2px; }
    li a {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 10px;
      border-radius: var(--radio);
      color: #c8cfda;
      text-decoration: none;
      font-weight: 500;
    }
    li a:hover { background: rgba(255, 255, 255, 0.06); color: #fff; }
    li a.activa {
      background: var(--grafito-2);
      color: #fff;
      box-shadow: inset 3px 0 0 #5cc0c7;
    }
    li a mat-icon { color: #8d97a6; }
    li a.activa mat-icon { color: #5cc0c7; }
    li a:focus-visible, .marca:focus-visible { outline-color: #5cc0c7; }
    .pie {
      margin-top: auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      border-top: 1px solid rgba(255, 255, 255, 0.1);
      padding: 14px 4px 0 8px;
    }
    .usuario { display: flex; align-items: center; gap: 10px; min-width: 0; }
    .inicial {
      width: 28px; height: 28px; flex: none;
      display: grid; place-items: center;
      border-radius: 50%;
      background: var(--grafito-2);
      color: #fff; font-weight: 600; font-size: 0.85rem; text-transform: uppercase;
    }
    .quien { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 0.9rem; }
    .salir { color: #c8cfda; }
    .contenido { min-width: 0; }

    @media (max-width: 860px) {
      .marco { grid-template-columns: 1fr; background: none; }
      .lateral {
        position: static; height: auto; flex-direction: row; flex-wrap: wrap; align-items: center;
        gap: 8px 16px; padding: 10px 16px; background: var(--grafito);
      }
      .nombre small { display: none; }
      ul { display: flex; flex-wrap: wrap; }
      li a span { display: none; }
      .pie { margin: 0 0 0 auto; border: none; padding: 0; }
      .quien { display: none; }
    }
  `,
})
export class Shell {
  protected readonly auth = inject(Auth);
  protected readonly secciones = [
    { ruta: '/encargos', nombre: 'Encargos', icono: 'inventory_2', exacta: false },
    { ruta: '/clientes', nombre: 'Clientes', icono: 'group', exacta: false },
    { ruta: '/modelos', nombre: 'Modelos', icono: 'deployed_code', exacta: false },
    { ruta: '/informes', nombre: 'Informes', icono: 'monitoring', exacta: false },
    { ruta: '/ajustes', nombre: 'Ajustes', icono: 'tune', exacta: false },
  ];

  protected inicial(): string {
    return (this.auth.usuario() ?? '?').charAt(0);
  }
}
