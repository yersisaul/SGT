import { Directive, input } from '@angular/core';

type SvgChildTag = 'path' | 'line' | 'polyline' | 'polygon' | 'circle' | 'ellipse' | 'rect';

/** Definición de un icono de Lucide: nombre y nodos hijos del <svg> (viewBox 24×24). */
export interface LucideIconNode {
  name: string;
  node: [SvgChildTag, Record<string, string>][];
}

/**
 * Atributos del <svg> idénticos a los de @lucide/angular: 24×24, trazo de
 * 2px en currentColor (hereda el color del texto) y clase `lucide` (se suma a las de la plantilla).
 */
@Directive({
  host: {
    xmlns: 'http://www.w3.org/2000/svg',
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    'stroke-width': '2',
    'stroke-linecap': 'round',
    'stroke-linejoin': 'round',
    '[attr.width]': 'size()',
    '[attr.height]': 'size()',
    class: 'lucide',
  },
})
export abstract class LucideIconBase {
  /** Ancho y alto en px. */
  readonly size = input<number | string>(24);
  protected abstract readonly icon: LucideIconNode;
}

/** Plantilla común: dibuja los nodos del icono dentro del <svg> anfitrión. */
export const LUCIDE_ICON_TEMPLATE = `
@for (child of icon.node; track $index) {
  @let a = child[1];
  @switch (child[0]) {
    @case ('path') { <svg:path [attr.d]="a['d']" /> }
    @case ('line') { <svg:line [attr.x1]="a['x1']" [attr.y1]="a['y1']" [attr.x2]="a['x2']" [attr.y2]="a['y2']" /> }
    @case ('polyline') { <svg:polyline [attr.points]="a['points']" /> }
    @case ('polygon') { <svg:polygon [attr.points]="a['points']" /> }
    @case ('circle') { <svg:circle [attr.cx]="a['cx']" [attr.cy]="a['cy']" [attr.r]="a['r']" /> }
    @case ('ellipse') { <svg:ellipse [attr.cx]="a['cx']" [attr.cy]="a['cy']" [attr.rx]="a['rx']" [attr.ry]="a['ry']" /> }
    @case ('rect') {
      <svg:rect [attr.x]="a['x']" [attr.y]="a['y']" [attr.width]="a['width']" [attr.height]="a['height']" [attr.rx]="a['rx']" [attr.ry]="a['ry']" />
    }
  }
}
`;
