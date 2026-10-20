import { DatePipe } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ModuloProducto, WebProductoRequest, WebProductoResponse } from '../../../../core/models/web.model';
import { PRODUCTOS } from '../../../../core/services/web-contenido.service';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { FileUpload } from '../../../../shared/components/file-upload/file-upload';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import {
  LucideCircleAlert,
  LucidePencil,
  LucidePlus,
  LucideStar,
  LucideTrash,
  LucideX,
} from '../../../../shared/icons/lucide-icons';
import { ListaTexto } from '../components/lista-texto';
import { PublicadoSwitch } from '../components/publicado-switch';
import { WebMiniatura } from '../components/web-miniatura';
import { WebToolbar } from '../components/web-toolbar';
import { SeccionWebBase } from '../seccion-web.base';

const URL_HTTPS = /^https:\/\/\S+$/;

/** Productos del sitio (Azor Panel, Azor Analytics…): /api/web/productos, permisos web_catalogo.*. */
@Component({
  selector: 'app-web-productos',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    Button,
    Dialog,
    FileUpload,
    Spinner,
    ListaTexto,
    PublicadoSwitch,
    WebMiniatura,
    WebToolbar,
    LucideCircleAlert,
    LucidePencil,
    LucidePlus,
    LucideStar,
    LucideTrash,
    LucideX,
  ],
  templateUrl: './productos.html',
  styleUrls: ['../../administracion/administracion.css', '../components/web-controles.css', '../web.css'],
})
export class Productos extends SeccionWebBase<WebProductoResponse, WebProductoRequest> {
  protected readonly recurso = PRODUCTOS;
  protected readonly grupoPermiso = 'web_catalogo';
  protected readonly entidad = 'producto';

  protected readonly form = new FormGroup({
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    slug: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    subtitulo: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(200)] }),
    descripcion_corta: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(300)] }),
    descripcion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(20000)] }),
    url_video: new FormControl('', { nonNullable: true, validators: [Validators.pattern(URL_HTTPS), Validators.maxLength(500)] }),
    orden: new FormControl(0, { nonNullable: true, validators: [Validators.min(0), Validators.max(9999)] }),
    destacado: new FormControl(false, { nonNullable: true }),
    publicado: new FormControl(false, { nonNullable: true }),
  });

  protected readonly caracteristicas = signal<string[]>([]);
  protected readonly modulos = signal<ModuloProducto[]>([]);

  protected id(item: WebProductoResponse): string {
    return item.id_producto;
  }

  protected nombre(item: WebProductoResponse): string {
    return item.nombre;
  }

  protected textoBusqueda(item: WebProductoResponse): string {
    return `${item.nombre} ${item.slug} ${item.subtitulo ?? ''}`;
  }

  protected cargarFormulario(item: WebProductoResponse | null): void {
    this.form.reset({
      nombre: item?.nombre ?? '',
      slug: item?.slug ?? '',
      subtitulo: item?.subtitulo ?? '',
      descripcion_corta: item?.descripcion_corta ?? '',
      descripcion: item?.descripcion ?? '',
      url_video: item?.url_video ?? '',
      orden: item?.orden ?? this.items().length,
      destacado: item?.destacado ?? false,
      publicado: item?.publicado ?? false,
    });
    this.caracteristicas.set([...(item?.caracteristicas ?? [])]);
    this.modulos.set((item?.modulos ?? []).map((modulo) => ({ ...modulo })));
  }

  protected construirRequest(): WebProductoRequest {
    const valor = this.form.getRawValue();
    return {
      nombre: valor.nombre.trim(),
      slug: this.texto(valor.slug),
      subtitulo: this.texto(valor.subtitulo),
      descripcion_corta: this.texto(valor.descripcion_corta),
      descripcion: this.texto(valor.descripcion),
      url_video: this.texto(valor.url_video),
      caracteristicas: this.limpiarLista(this.caracteristicas()),
      modulos: this.modulos()
        .map((modulo) => ({
          nombre: modulo.nombre.trim(),
          grupo: this.texto(modulo.grupo),
          descripcion: this.texto(modulo.descripcion),
        }))
        .filter((modulo) => modulo.nombre !== ''),
      orden: valor.orden,
      destacado: valor.destacado,
      publicado: valor.publicado,
    };
  }

  protected agregarModulo(): void {
    this.modulos.update((lista) => [...lista, { nombre: '', grupo: lista.at(-1)?.grupo ?? '', descripcion: '' }]);
  }

  protected editarModulo(indice: number, campo: keyof ModuloProducto, valor: string): void {
    this.modulos.update((lista) => lista.map((modulo, i) => (i === indice ? { ...modulo, [campo]: valor } : modulo)));
  }

  protected quitarModulo(indice: number): void {
    this.modulos.update((lista) => lista.filter((_, i) => i !== indice));
  }
}
