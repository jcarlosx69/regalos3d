import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, of, startWith, switchMap, tap } from 'rxjs';
import { Api } from '../../core/api';
import { Cliente } from '../../core/modelos-api';
import { Avisos } from '../../shared/avisos';
import { confirmar } from '../../shared/confirmar';
import { TelefonoPipe } from '../../shared/formato';
import { abrirClienteDialog } from './cliente-dialog';

@Component({
  selector: 'app-clientes-lista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    MatTooltipModule, MatProgressBarModule, TelefonoPipe,
  ],
  template: `
    <div class="pagina">
      <header class="pagina-cabecera">
        <div>
          <h1>Clientes</h1>
          <p>Compradores y destinatarios de regalos. El móvil identifica a cada uno.</p>
        </div>
        <button mat-flat-button (click)="nuevo()"><mat-icon>person_add</mat-icon> Nuevo cliente</button>
      </header>

      <div class="herramientas">
        <mat-form-field appearance="outline" class="buscador">
          <mat-icon matPrefix>search</mat-icon>
          <mat-label>Buscar por nombre, móvil o email</mat-label>
          <input matInput [formControl]="busqueda" />
        </mat-form-field>
        @if (!cargando()) { <span class="recuento">{{ clientes().length }} clientes</span> }
      </div>

      @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }

      @if (clientes().length) {
        <div class="tabla-scroll">
          <table class="tabla">
            <thead>
              <tr>
                <th>Nombre</th><th>Móvil</th><th>Email</th><th>Dirección</th>
                <th class="acciones"><span class="cdk-visually-hidden">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              @for (c of clientes(); track c.id) {
                <tr class="clicable" (click)="abrir(c)" (keydown.enter)="abrir(c)" tabindex="0">
                  <td><strong>{{ c.nombre }}</strong></td>
                  <td class="cifra nowrap">{{ c.telefono | telefono }}</td>
                  <td>{{ c.email || '—' }}</td>
                  <td class="suave recorte">{{ c.direccion || '—' }}</td>
                  <td class="acciones" (click)="$event.stopPropagation()" (keydown.enter)="$event.stopPropagation()">
                    <button mat-icon-button (click)="editar(c)" matTooltip="Editar" [attr.aria-label]="'Editar ' + c.nombre">
                      <mat-icon>edit</mat-icon>
                    </button>
                    <button mat-icon-button (click)="borrar(c)" matTooltip="Eliminar" [attr.aria-label]="'Eliminar ' + c.nombre">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      } @else if (!cargando()) {
        @if (busqueda.value) {
          <div class="vacio">
            <h2>Sin resultados para «{{ busqueda.value }}»</h2>
            <p>La búsqueda compara el nombre, el email y cualquier parte del número.</p>
          </div>
        } @else {
          <div class="vacio">
            <h2>No hay clientes registrados</h2>
            <p>Da de alta a compradores y destinatarios con su nombre y móvil para poder registrar encargos.</p>
            <button mat-flat-button (click)="nuevo()"><mat-icon>person_add</mat-icon> Nuevo cliente</button>
          </div>
        }
      }
    </div>
  `,
  styles: `
    .nowrap { white-space: nowrap; }
    .recorte { max-width: 32ch; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `,
})
export class ClientesLista {
  private readonly api = inject(Api);
  private readonly dialog = inject(MatDialog);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);

  protected readonly busqueda = new FormControl('', { nonNullable: true });
  protected readonly clientes = signal<Cliente[]>([]);
  protected readonly cargando = signal(true);

  constructor() {
    this.busqueda.valueChanges
      .pipe(
        startWith(''),
        debounceTime(250),
        distinctUntilChanged(),
        tap(() => this.cargando.set(true)),
        switchMap((q) =>
          this.api.clientes(q.trim()).pipe(
            catchError((e) => {
              this.avisos.error(e);
              return of([] as Cliente[]);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((cs) => {
        this.clientes.set(cs);
        this.cargando.set(false);
      });
  }

  private recargar(): void {
    this.api.clientes(this.busqueda.value.trim()).subscribe((cs) => this.clientes.set(cs));
  }

  protected abrir(c: Cliente): void {
    this.router.navigate(['/clientes', c.id]);
  }

  protected nuevo(): void {
    abrirClienteDialog(this.dialog).subscribe((c) => {
      if (c) {
        this.avisos.hecho('Cliente registrado');
        this.recargar();
      }
    });
  }

  protected editar(c: Cliente): void {
    abrirClienteDialog(this.dialog, c).subscribe((g) => {
      if (g) {
        this.avisos.hecho('Cambios guardados');
        this.recargar();
      }
    });
  }

  protected borrar(c: Cliente): void {
    confirmar(this.dialog, {
      titulo: `¿Eliminar a ${c.nombre}?`,
      texto: 'Solo se puede eliminar a clientes sin encargos registrados.',
      aceptar: 'Eliminar',
      peligroso: true,
    }).subscribe((ok) => {
      if (!ok) return;
      this.api.borrarCliente(c.id).subscribe({
        next: () => {
          this.avisos.hecho('Cliente eliminado');
          this.recargar();
        },
        error: (e) => this.avisos.error(e),
      });
    });
  }
}
