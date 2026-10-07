import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Colores habituales de filamento, por su nombre en castellano. */
const COLORES: Record<string, string> = {
  blanco: '#f7f7f4', negro: '#1d1d1d', gris: '#8a8f8d', 'gris claro': '#c4c8c6', 'gris oscuro': '#4d5150',
  plata: '#c0c4c4', plateado: '#c0c4c4', oro: '#c9a227', dorado: '#c9a227', bronce: '#a2703a', cobre: '#b8673a',
  rojo: '#c8242c', granate: '#7a1f2b', burdeos: '#6d1a2a', rosa: '#f29bb8', fucsia: '#d6247c', magenta: '#c2187a',
  naranja: '#ee7a1d', amarillo: '#f5cf1d', crema: '#efe3c4', beige: '#d9c7a3', marron: '#6e4a2f', 'marrón': '#6e4a2f',
  madera: '#a8774a', verde: '#2f9a4b', 'verde claro': '#8fd18a', 'verde oscuro': '#1f5b33', oliva: '#6f7a32',
  menta: '#a8e3c8', turquesa: '#25b3b0', cian: '#22a7d6', azul: '#2560c2', 'azul claro': '#7fb6ea',
  'azul oscuro': '#1d3466', marino: '#1d2a52', morado: '#6b3fa0', lila: '#b79bd9', violeta: '#7d47b5',
  transparente: 'transparent', natural: '#e9e4d4',
};

/** Muestra circular del color de una bobina. Si el nombre no se reconoce, se dibuja con trama neutra. */
@Component({
  selector: 'app-muestra-color',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="muestra" [class.desconocido]="!hex()" [class.transparente]="hex() === 'transparent'"
                   [style.background]="hex() && hex() !== 'transparent' ? hex() : null"
                   [attr.title]="nombre() || 'Sin color'"></span>`,
  styles: `
    :host { display: inline-flex; }
    .muestra {
      width: var(--tam, 14px); height: var(--tam, 14px);
      border-radius: 50%;
      border: 1px solid rgba(24, 48, 47, 0.35);
      box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.35);
    }
    .desconocido { background: repeating-linear-gradient(45deg, #e3e8e5 0 3px, #c9d1cd 3px 6px); }
    .transparente { background: conic-gradient(#fff 0 25%, #dfe5e2 0 50%, #fff 0 75%, #dfe5e2 0) 0 0 / 7px 7px; }
  `,
})
export class MuestraColor {
  readonly nombre = input<string | null | undefined>();
  protected readonly hex = computed(() => {
    const n = (this.nombre() ?? '').trim().toLowerCase();
    if (!n) return null;
    if (/^#[0-9a-f]{3,8}$/.test(n)) return n;
    return COLORES[n] ?? COLORES[n.split(' ')[0]] ?? null;
  });
}
