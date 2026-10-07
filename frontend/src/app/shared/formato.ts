import { Pipe, PipeTransform } from '@angular/core';

/** +34611223344 → +34 611 22 33 44 */
@Pipe({ name: 'telefono' })
export class TelefonoPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    if (!valor) return '';
    const m = /^\+34(\d{3})(\d{2})(\d{2})(\d{2})$/.exec(valor);
    return m ? `+34 ${m[1]} ${m[2]} ${m[3]} ${m[4]}` : valor;
  }
}

const EUROS = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR', minimumFractionDigits: 2 });

/** 0.77 → 0,77 € */
@Pipe({ name: 'euros' })
export class EurosPipe implements PipeTransform {
  transform(valor: number | null | undefined): string {
    return valor == null ? '—' : EUROS.format(valor);
  }
}

/** '2026-03-12' → 12/03/2026, sin conversiones de zona horaria. */
export function fechaCorta(iso: string | null | undefined): string {
  if (!iso) return '';
  const [a, m, d] = iso.split('-');
  return `${d}/${m}/${a}`;
}

@Pipe({ name: 'fecha' })
export class FechaPipe implements PipeTransform {
  transform(iso: string | null | undefined): string {
    return fechaCorta(iso);
  }
}

/** Fecha de hoy en formato yyyy-MM-dd, en hora local. */
export function hoyIso(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}

const NUMERO = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 2 });

/** 3.5 → 3,5 (hasta dos decimales, formato español) */
@Pipe({ name: 'numero' })
export class NumeroPipe implements PipeTransform {
  transform(valor: number | null | undefined): string {
    return NUMERO.format(valor ?? 0);
  }
}
