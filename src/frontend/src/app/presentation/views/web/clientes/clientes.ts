import { DatePipe } from '@angular/common';
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { WebClienteReferenciaRequest, WebClienteReferenciaResponse } from '../../../../core/models/web.model';
import { CLIENTES } from '../../../../core/services/web-contenido.service';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { FileUpload } from '../../../../shared/components/file-upload/file-upload';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import {
  LucideCircleAlert,
  LucideExternalLink,
  LucidePencil,
  LucidePlus,
  LucideTrash,
} from '../../../../shared/icons/lucide-icons';
import { PublicadoSwitch } from '../components/publicado-switch';
import { WebMiniatura } from '../components/web-miniatura';
import { WebToolbar } from '../components/web-toolbar';
import { SeccionWebBase } from '../seccion-web.base';

const URL_WEB = /^https?:\/\/\S+$/;

/** Clientes de referencia (franja de logos del sitio): /api/web/clientes, permisos web_comercial.*. */
@Component({
  selector: 'app-web-clientes',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    Button,
    Dialog,
    FileUpload,
    Spinner,
    PublicadoSwitch,
    WebMiniatura,
    WebToolbar,
    LucideCircleAlert,
    LucideExternalLink,
    LucidePencil,
    LucidePlus,
    LucideTrash,
  ],
  templateUrl: './clientes.html',
  styleUrls: ['../../administracion/administracion.css', '../components/web-controles.css', '../web.css'],
})
export class Clientes extends SeccionWebBase<WebClienteReferenciaResponse, WebClienteReferenciaRequest> {
  protected readonly recurso = CLIENTES;
  protected readonly grupoPermiso = 'web_comercial';
  protected readonly entidad = 'cliente';

  protected readonly form = new FormGroup({
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    sector: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(100)] }),
    url_sitio: new FormControl('', { nonNullable: true, validators: [Validators.pattern(URL_WEB), Validators.maxLength(500)] }),
    orden: new FormControl(0, { nonNullable: true, validators: [Validators.min(0), Validators.max(9999)] }),
    publicado: new FormControl(false, { nonNullable: true }),
  });

  protected id(item: WebClienteReferenciaResponse): string {
    return item.id_cliente_referencia;
  }

  protected nombre(item: WebClienteReferenciaResponse): string {
    return item.nombre;
  }

  protected textoBusqueda(item: WebClienteReferenciaResponse): string {
    return `${item.nombre} ${item.sector ?? ''}`;
  }

  protected cargarFormulario(item: WebClienteReferenciaResponse | null): void {
    this.form.reset({
      nombre: item?.nombre ?? '',
      sector: item?.sector ?? '',
      url_sitio: item?.url_sitio ?? '',
      orden: item?.orden ?? this.items().length,
      publicado: item?.publicado ?? false,
    });
  }

  protected construirRequest(): WebClienteReferenciaRequest {
    const valor = this.form.getRawValue();
    return {
      nombre: valor.nombre.trim(),
      sector: this.texto(valor.sector),
      url_sitio: this.texto(valor.url_sitio),
      orden: valor.orden,
      publicado: valor.publicado,
    };
  }
}
