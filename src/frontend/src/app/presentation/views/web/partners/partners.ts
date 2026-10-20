import { DatePipe } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { TIPO_PARTNER_LABEL, TipoPartner, WebPartnerRequest, WebPartnerResponse } from '../../../../core/models/web.model';
import { PARTNERS } from '../../../../core/services/web-contenido.service';
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

/** Partners (alianzas tecnológicas y marcas compatibles): /api/web/partners, permisos web_comercial.*. */
@Component({
  selector: 'app-web-partners',
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
  templateUrl: './partners.html',
  styleUrls: ['../../administracion/administracion.css', '../components/web-controles.css', '../web.css'],
})
export class Partners extends SeccionWebBase<WebPartnerResponse, WebPartnerRequest> {
  protected readonly recurso = PARTNERS;
  protected readonly grupoPermiso = 'web_comercial';
  protected readonly entidad = 'partner';
  protected readonly tipoLabel = TIPO_PARTNER_LABEL;
  protected readonly tipos = Object.keys(TIPO_PARTNER_LABEL) as TipoPartner[];

  /** Pestañas por tipo además del filtro de publicación. */
  protected readonly tipoFiltro = signal<TipoPartner | 'TODOS'>('TODOS');
  protected readonly visibles = computed(() => {
    const tipo = this.tipoFiltro();
    return this.filtrados().filter((item) => tipo === 'TODOS' || item.tipo === tipo);
  });

  protected readonly form = new FormGroup({
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    tipo: new FormControl<TipoPartner>('TECNOLOGICO', { nonNullable: true }),
    categoria: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(100)] }),
    descripcion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(1000)] }),
    url_sitio: new FormControl('', { nonNullable: true, validators: [Validators.pattern(URL_WEB), Validators.maxLength(500)] }),
    orden: new FormControl(0, { nonNullable: true, validators: [Validators.min(0), Validators.max(9999)] }),
    publicado: new FormControl(false, { nonNullable: true }),
  });

  protected contarTipo(tipo: TipoPartner): number {
    return this.filtrados().filter((item) => item.tipo === tipo).length;
  }

  protected id(item: WebPartnerResponse): string {
    return item.id_partner;
  }

  protected nombre(item: WebPartnerResponse): string {
    return item.nombre;
  }

  protected textoBusqueda(item: WebPartnerResponse): string {
    return `${item.nombre} ${item.categoria ?? ''}`;
  }

  protected cargarFormulario(item: WebPartnerResponse | null): void {
    const tipo = item?.tipo ?? (this.tipoFiltro() === 'TODOS' ? 'TECNOLOGICO' : (this.tipoFiltro() as TipoPartner));
    this.form.reset({
      nombre: item?.nombre ?? '',
      tipo,
      categoria: item?.categoria ?? '',
      descripcion: item?.descripcion ?? '',
      url_sitio: item?.url_sitio ?? '',
      orden: item?.orden ?? this.items().filter((otro) => otro.tipo === tipo).length,
      publicado: item?.publicado ?? false,
    });
  }

  protected construirRequest(): WebPartnerRequest {
    const valor = this.form.getRawValue();
    return {
      nombre: valor.nombre.trim(),
      tipo: valor.tipo,
      categoria: this.texto(valor.categoria),
      descripcion: this.texto(valor.descripcion),
      url_sitio: this.texto(valor.url_sitio),
      orden: valor.orden,
      publicado: valor.publicado,
    };
  }
}
