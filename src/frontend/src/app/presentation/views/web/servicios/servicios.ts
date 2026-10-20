import { DatePipe } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { WebServicioRequest, WebServicioResponse } from '../../../../core/models/web.model';
import { SERVICIOS } from '../../../../core/services/web-contenido.service';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { FileUpload } from '../../../../shared/components/file-upload/file-upload';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import { LucideCircleAlert, LucidePencil, LucidePlus, LucideTrash } from '../../../../shared/icons/lucide-icons';
import { ListaTexto } from '../components/lista-texto';
import { PublicadoSwitch } from '../components/publicado-switch';
import { WebMiniatura } from '../components/web-miniatura';
import { WebToolbar } from '../components/web-toolbar';
import { SeccionWebBase } from '../seccion-web.base';

/** Servicios del sitio (servidores, VMS, modelos de visión…): /api/web/servicios, permisos web_catalogo.*. */
@Component({
  selector: 'app-web-servicios',
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
    LucideTrash,
  ],
  templateUrl: './servicios.html',
  styleUrls: ['../../administracion/administracion.css', '../components/web-controles.css', '../web.css'],
})
export class Servicios extends SeccionWebBase<WebServicioResponse, WebServicioRequest> {
  protected readonly recurso = SERVICIOS;
  protected readonly grupoPermiso = 'web_catalogo';
  protected readonly entidad = 'servicio';

  protected readonly form = new FormGroup({
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    slug: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    icono: new FormControl('', { nonNullable: true, validators: [Validators.pattern(/^[a-z0-9-]*$/), Validators.maxLength(60)] }),
    descripcion_corta: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(300)] }),
    descripcion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(20000)] }),
    orden: new FormControl(0, { nonNullable: true, validators: [Validators.min(0), Validators.max(9999)] }),
    publicado: new FormControl(false, { nonNullable: true }),
  });

  protected readonly caracteristicas = signal<string[]>([]);

  protected id(item: WebServicioResponse): string {
    return item.id_servicio;
  }

  protected nombre(item: WebServicioResponse): string {
    return item.nombre;
  }

  protected textoBusqueda(item: WebServicioResponse): string {
    return `${item.nombre} ${item.slug} ${item.descripcion_corta ?? ''}`;
  }

  protected cargarFormulario(item: WebServicioResponse | null): void {
    this.form.reset({
      nombre: item?.nombre ?? '',
      slug: item?.slug ?? '',
      icono: item?.icono ?? '',
      descripcion_corta: item?.descripcion_corta ?? '',
      descripcion: item?.descripcion ?? '',
      orden: item?.orden ?? this.items().length,
      publicado: item?.publicado ?? false,
    });
    this.caracteristicas.set([...(item?.caracteristicas ?? [])]);
  }

  protected construirRequest(): WebServicioRequest {
    const valor = this.form.getRawValue();
    return {
      nombre: valor.nombre.trim(),
      slug: this.texto(valor.slug),
      icono: this.texto(valor.icono),
      descripcion_corta: this.texto(valor.descripcion_corta),
      descripcion: this.texto(valor.descripcion),
      caracteristicas: this.limpiarLista(this.caracteristicas()),
      orden: valor.orden,
      publicado: valor.publicado,
    };
  }
}
