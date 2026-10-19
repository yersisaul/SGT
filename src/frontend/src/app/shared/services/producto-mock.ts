import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

export interface Producto {
  id?: number;
  nombre: string;
  descripcion: string;
  precio: number;
  categoria: string;
  imagen: string;
}

@Injectable({ providedIn: 'root' })
export class ProductoMock {
  private nextId = 4;
  private data: Producto[] = [
    { id: 1, nombre: 'Servidor Rack 2U', descripcion: 'Alto rendimiento', precio: 5200, categoria: 'Servidores', imagen: 'images/img1.jpg' },
    { id: 2, nombre: 'Cámara IA 4K', descripcion: 'Analítica de video', precio: 800, categoria: 'Video', imagen: 'images/img2.jpg' },
    { id: 3, nombre: 'Switch 24 puertos', descripcion: 'Gigabit gestionable', precio: 450, categoria: 'Redes', imagen: 'images/img3.jpg' },
  ];

  getAll(): Observable<Producto[]> {
    return of([...this.data]);
  }

  create(p: Producto): Observable<Producto> {
    const nuevo = { ...p, id: this.nextId++ };
    this.data.push(nuevo);
    return of(nuevo);
  }

  update(id: number, p: Producto): Observable<Producto> {
    const actualizado = { ...p, id };
    this.data = this.data.map(x => (x.id === id ? actualizado : x));
    return of(actualizado);
  }

  delete(id: number): Observable<void> {
    this.data = this.data.filter(x => x.id !== id);
    return of(void 0);
  }
}
