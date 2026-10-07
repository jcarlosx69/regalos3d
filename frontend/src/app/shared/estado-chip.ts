import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { NOMBRE_ESTADO } from '../core/estados';
import { EstadoEncargo } from '../core/modelos-api';

/** Etiqueta de estado de un encargo, con un color por fase del flujo. */
@Component({
  selector: 'app-estado-chip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="chip" [attr.data-estado]="estado()">{{ nombre() }}</span>`,
  styles: `
    .chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 2px 9px 2px 8px;
      border-radius: 999px;
      font-size: 0.8125rem;
      font-weight: 600;
      white-space: nowrap;
      line-height: 1.5;
      color: var(--c, #3b4452);
      background: var(--f, #eceff3);
    }
    .chip::before {
      content: '';
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: currentColor;
    }
    [data-estado='PRESUPUESTO'] { --c: #5b6472; --f: #eceff3; }
    [data-estado='ACEPTADO'] { --c: #1d4f91; --f: #e4edf9; }
    [data-estado='EN_COLA'] { --c: #5a3d99; --f: #efe9f8; }
    [data-estado='IMPRIMIENDO'] { --c: #0e5a62; --f: #dff1f2; }
    [data-estado='TERMINADO'] { --c: #7a5a00; --f: #fbf1d6; }
    [data-estado='ENTREGADO'] { --c: #1f6b3a; --f: #e2f3e7; }
    [data-estado='COBRADO'] { --c: #ffffff; --f: #1f6b3a; }
    [data-estado='CANCELADO'] { --c: #8a2a22; --f: #f8e6e4; text-decoration: line-through; }
  `,
})
export class EstadoChip {
  readonly estado = input.required<EstadoEncargo>();
  protected readonly nombre = computed(() => NOMBRE_ESTADO[this.estado()]);
}
