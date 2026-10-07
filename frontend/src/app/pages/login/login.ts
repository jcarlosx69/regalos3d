import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { Auth } from '../../core/auth';
import { mensajeError } from '../../shared/avisos';
import { Logo } from '../../shared/logo';

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, Logo],
  template: `
    <main class="pantalla">
      <section class="panel" aria-labelledby="titulo-login">
        <div class="marca">
          <app-logo />
          <div>
            <p class="producto">Regalos 3D</p>
            <p class="suave">Gestión de impresiones</p>
          </div>
        </div>

        <h1 id="titulo-login">Iniciar sesión</h1>

        <form [formGroup]="form" (ngSubmit)="entrar()">
          <mat-form-field appearance="outline">
            <mat-label>Usuario</mat-label>
            <input matInput formControlName="usuario" autocomplete="username" required />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Contraseña</mat-label>
            <input matInput [type]="ver() ? 'text' : 'password'" formControlName="password"
                   autocomplete="current-password" required />
            <button mat-icon-button matSuffix type="button" (click)="ver.set(!ver())"
                    [attr.aria-label]="ver() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
              <mat-icon>{{ ver() ? 'visibility_off' : 'visibility' }}</mat-icon>
            </button>
          </mat-form-field>

          @if (error()) {
            <p class="error" role="alert">{{ error() }}</p>
          }

          <button mat-flat-button type="submit" [disabled]="enviando()">
            {{ enviando() ? 'Entrando…' : 'Entrar' }}
          </button>
        </form>
      </section>
      <p class="nota">Acceso disponible solo desde la red local.</p>
    </main>
  `,
  styles: `
    .pantalla {
      min-height: 100vh;
      display: grid;
      place-content: center;
      justify-items: center;
      gap: 16px;
      padding: 24px 16px;
      background: var(--grafito);
    }
    .panel {
      width: min(400px, calc(100vw - 32px));
      background: var(--superficie);
      border-radius: var(--radio-panel);
      padding: 28px 32px 28px;
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.28);
    }
    .marca {
      display: flex;
      align-items: center;
      gap: 12px;
      padding-bottom: 20px;
      margin-bottom: 22px;
      border-bottom: 1px solid var(--linea);
      --tam-logo: 38px;

      p { margin: 0; line-height: 1.2; }
    }
    .producto { font-weight: 700; font-size: 1.15rem; font-stretch: 90%; }
    .marca .suave { font-size: 0.85rem; }
    h1 { font-size: 1.3rem; margin-bottom: 16px; }
    form { display: grid; gap: 4px; }
    .error { color: var(--peligro); margin: 0 0 12px; }
    button[type='submit'] { height: 42px; }
    .nota { margin: 0; color: #8d97a6; font-size: 0.85rem; }
  `,
})
export class Login implements OnInit {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  /** Ruta a la que volver tras entrar (?volver=/encargos/nuevo). */
  readonly volver = input<string>();

  protected readonly ver = signal(false);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    usuario: ['', Validators.required],
    password: ['', Validators.required],
  });

  ngOnInit(): void {
    // Si ya hay sesión, no tiene sentido mostrar el login. De paso se recibe la cookie CSRF.
    this.auth.comprobar().subscribe((ok) => ok && this.irDentro());
  }

  protected entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { usuario, password } = this.form.getRawValue();
    this.enviando.set(true);
    this.error.set(null);
    this.auth.entrar(usuario, password).subscribe({
      next: (ok) => {
        this.enviando.set(false);
        if (ok) this.irDentro();
        else this.error.set('Usuario o contraseña incorrectos.');
      },
      error: (e) => {
        this.enviando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }

  private irDentro(): void {
    const destino = this.volver();
    this.router.navigateByUrl(destino && destino.startsWith('/') ? destino : '/encargos');
  }
}
