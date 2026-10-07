import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { MuestraColor } from './muestra-color';

function pintar(nombre: string | null) {
  const f = TestBed.createComponent(MuestraColor);
  f.componentRef.setInput('nombre', nombre);
  f.detectChanges();
  return f.nativeElement.querySelector('.muestra') as HTMLElement;
}

describe('MuestraColor', () => {
  it('reconoce colores de filamento en castellano, sin importar mayúsculas', () => {
    expect(pintar('Rosa').style.background).not.toBe('');
    expect(pintar('AZUL OSCURO').classList.contains('desconocido')).toBe(false);
  });

  it('usa la primera palabra si el nombre completo no está en la lista', () => {
    expect(pintar('verde pistacho').classList.contains('desconocido')).toBe(false);
  });

  it('acepta un código hexadecimal', () => {
    expect(pintar('#ff0000').style.background).toContain('rgb(255, 0, 0)');
  });

  it('dibuja una trama cuando el color no se conoce o falta', () => {
    expect(pintar('Galaxia').classList.contains('desconocido')).toBe(true);
    expect(pintar(null).classList.contains('desconocido')).toBe(true);
  });
});
