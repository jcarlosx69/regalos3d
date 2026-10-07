import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Cubo isométrico con líneas de capa: una pieza impresa por deposición. */
@Component({
  selector: 'app-logo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg viewBox="0 0 32 32" aria-hidden="true">
      <polygon points="16,3 28,10 16,17 4,10" fill="#5cc0c7" />
      <polygon points="4,10 16,17 16,30 4,23" fill="#0e5a62" />
      <polygon points="16,17 28,10 28,23 16,30" fill="#0a4449" />
      <g stroke="#ffffff" stroke-opacity="0.32" stroke-width="0.8">
        <line x1="4" y1="13.25" x2="16" y2="20.25" />
        <line x1="4" y1="16.5" x2="16" y2="23.5" />
        <line x1="4" y1="19.75" x2="16" y2="26.75" />
        <line x1="16" y1="20.25" x2="28" y2="13.25" />
        <line x1="16" y1="23.5" x2="28" y2="16.5" />
        <line x1="16" y1="26.75" x2="28" y2="19.75" />
      </g>
    </svg>
  `,
  styles: `
    :host { display: inline-flex; width: var(--tam-logo, 28px); height: var(--tam-logo, 28px); flex: none; }
    svg { width: 100%; height: 100%; }
  `,
})
export class Logo {}
