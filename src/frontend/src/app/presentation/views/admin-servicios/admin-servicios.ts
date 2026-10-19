import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ServicioMock, Servicio } from '../../../shared/services/servicio-mock';

@Component({
  selector: 'app-admin-servicios',
  standalone: true,
  imports: [FormsModule, RouterLink, RouterLinkActive],
  templateUrl: './admin-servicios.html',
  styleUrl: '../admin-productos/admin-productos.css', // reutiliza el mismo CSS
})
export class AdminServicios implements OnInit {
  private service = inject(ServicioMock);

  servicios: Servicio[] = [];
  busqueda = '';
  mostrarForm = false;
  editandoId: number | null = null;
  form: Servicio = this.vacio();
  caracteristicasTxt = '';

  ngOnInit() { this.cargar(); }

  vacio(): Servicio {
    return { nombre: '', descripcion_corta: '', descripcion: '', icono: '', caracteristicas: [], orden: 0, publicado: false, url_img: 'images/img1.jpg' };
  }

  cargar() { this.service.getAll().subscribe(s => (this.servicios = s)); }

  get publicados(): number { return this.servicios.filter(s => s.publicado).length; }

  get filtrados(): Servicio[] {
    const q = this.busqueda.toLowerCase().trim();
    return this.servicios.filter(s =>
      s.nombre.toLowerCase().includes(q) || s.descripcion_corta.toLowerCase().includes(q));
  }

  nuevo() {
    this.editandoId = null;
    this.form = this.vacio();
    this.caracteristicasTxt = '';
    this.mostrarForm = true;
  }

  editar(s: Servicio) {
    this.editandoId = s.id!;
    this.form = { ...s };
    this.caracteristicasTxt = s.caracteristicas.join('\n');
    this.mostrarForm = true;
  }

  guardar() {
    const body: Servicio = {
      ...this.form,
      caracteristicas: this.caracteristicasTxt.split('\n').map(x => x.trim()).filter(Boolean),
    };
    const op = this.editandoId
      ? this.service.update(this.editandoId, body)
      : this.service.create(body);
    op.subscribe(() => { this.cerrar(); this.cargar(); });
  }

  alternar(s: Servicio) {
    this.service.update(s.id!, { ...s, publicado: !s.publicado }).subscribe(() => this.cargar());
  }

  eliminar(id: number) {
    if (confirm('¿Eliminar este servicio?')) this.service.delete(id).subscribe(() => this.cargar());
  }

  cerrar() { this.mostrarForm = false; this.editandoId = null; this.form = this.vacio(); }
}
