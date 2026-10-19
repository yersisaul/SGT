import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

export interface Servicio {
  id?: number;
  nombre: string;
  descripcion_corta: string;
  descripcion: string;
  icono: string;
  caracteristicas: string[];
  orden: number;
  publicado: boolean;
  url_img: string;
}

@Injectable({ providedIn: 'root' })
export class ServicioMock {
  private nextId = 4;
  private data: Servicio[] = [
    { id: 1, nombre: 'Monitoreo 24/7', descripcion_corta: 'Vigilancia continua', descripcion: '', icono: 'activity', caracteristicas: ['Alertas en tiempo real', 'Reportes mensuales'], orden: 1, publicado: true, url_img: 'images/img1.jpg' },
    { id: 2, nombre: 'Soporte técnico', descripcion_corta: 'Atención por tickets con SLA', descripcion: '', icono: 'headset', caracteristicas: ['Respuesta en 4 h'], orden: 2, publicado: true, url_img: 'images/img2.jpg' },
    { id: 3, nombre: 'Consultoría cloud', descripcion_corta: 'Migración y optimización', descripcion: '', icono: 'cloud', caracteristicas: [], orden: 3, publicado: false, url_img: 'images/img3.jpg' },
  ];

  getAll(): Observable<Servicio[]> { return of([...this.data]); }

  create(s: Servicio): Observable<Servicio> {
    const nuevo = { ...s, id: this.nextId++ };
    this.data.push(nuevo);
    return of(nuevo);
  }

  update(id: number, s: Servicio): Observable<Servicio> {
    const actualizado = { ...s, id };
    this.data = this.data.map(x => (x.id === id ? actualizado : x));
    return of(actualizado);
  }

  delete(id: number): Observable<void> {
    this.data = this.data.filter(x => x.id !== id);
    return of(void 0);
  }
}
