import { Component, OnInit, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProductoMock, Producto } from '../../../shared/services/producto-mock';

@Component({
  selector: 'app-admin-productos',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, RouterLink],
  templateUrl: './admin-productos.html',
  styleUrl: './admin-productos.css',
})
export class AdminProductos implements OnInit {
  private service = inject(ProductoMock);

  productos: Producto[] = [];
  busqueda = '';
  categoriaFiltro = '';
  mostrarForm = false;
  editandoId: number | null = null;
  form: Producto = this.vacio();

  ngOnInit() {
    this.cargar();
  }

  vacio(): Producto {
    return { nombre: '', descripcion: '', precio: 0, categoria: '', imagen: 'images/img1.jpg' };
  }

  cargar() {
    this.service.getAll().subscribe(p => (this.productos = p));
  }

  get categorias(): string[] {
    return [...new Set(this.productos.map(p => p.categoria))];
  }

  get valorTotal(): number {
    return this.productos.reduce((acc, p) => acc + p.precio, 0);
  }

  get filtrados(): Producto[] {
    const q = this.busqueda.toLowerCase().trim();
    return this.productos.filter(
      p =>
        (!this.categoriaFiltro || p.categoria === this.categoriaFiltro) &&
        (p.nombre.toLowerCase().includes(q) || p.categoria.toLowerCase().includes(q)),
    );
  }

  nuevo() {
    this.editandoId = null;
    this.form = this.vacio();
    this.mostrarForm = true;
  }

  editar(p: Producto) {
    this.editandoId = p.id!;
    this.form = { ...p };
    this.mostrarForm = true;
  }

  guardar() {
    const op = this.editandoId
      ? this.service.update(this.editandoId, this.form)
      : this.service.create(this.form);
    op.subscribe(() => {
      this.cerrar();
      this.cargar();
    });
  }

  eliminar(id: number) {
    if (confirm('¿Eliminar este producto?')) {
      this.service.delete(id).subscribe(() => this.cargar());
    }
  }

  cerrar() {
    this.mostrarForm = false;
    this.editandoId = null;
    this.form = this.vacio();
  }

  limpiarFiltros() {
    this.busqueda = '';
    this.categoriaFiltro = '';
  }
}
