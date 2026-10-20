import { DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  MetricaCaso,
  WebCasoExitoRequest,
  WebCasoExitoResponse,
  WebClienteReferenciaResponse,
  WebProductoResponse,
} from '../../../../core/models/web.model';
import { CASOS_EXITO, CLIENTES, PRODUCTOS } from '../../../../core/services/web-contenido.service';
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
import { PublicadoSwitch } from '../components/publicado-switch';
import { WebMiniatura } from '../components/web-miniatura';
import { WebToolbar } from '../components/web-toolbar';
import { SeccionWebBase } from '../seccion-web.base';

const URL_WEB = /^https?:\/\/\S+$/;

/** Casos de éxito: /api/web/casos-exito, permisos web_comercial.* (productos: web_catalogo.read). */
@Component({
  selector: 'app-web-casos-exito',
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
    LucidePencil,
    LucidePlus,
    LucideStar,
    LucideTrash,
    LucideX,
  ],
  templateUrl: './casos-exito.html',
  styleUrls: ['../../administracion/administracion.css', '../components/web-controles.css', '../web.css'],
})
export class CasosExito extends SeccionWebBase<WebCasoExitoResponse, WebCasoExitoRequest> implements OnInit {
  protected readonly recurso = CASOS_EXITO;
  protected readonly grupoPermiso = 'web_comercial';
  protected readonly entidad = 'caso de éxito';

  protected readonly clientes = signal<WebClienteReferenciaResponse[]>([]);
  protected readonly productos = signal<WebProductoResponse[]>([]);
  protected readonly metricas = signal<MetricaCaso[]>([]);

  protected readonly form = new FormGroup({
    titulo: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    slug: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    resumen: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    contenido: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(20000)] }),
    url_externa: new FormControl('', { nonNullable: true, validators: [Validators.pattern(URL_WEB), Validators.maxLength(500)] }),
    fecha_caso: new FormControl('', { nonNullable: true }),
    id_cliente_referencia: new FormControl('', { nonNullable: true }),
    id_producto: new FormControl('', { nonNullable: true }),
    orden: new FormControl(0, { nonNullable: true, validators: [Validators.min(0), Validators.max(9999)] }),
    destacado: new FormControl(false, { nonNullable: true }),
    publicado: new FormControl(false, { nonNullable: true }),
  });

  override ngOnInit(): void {
    super.ngOnInit();
    // Catálogos para los vínculos (best-effort: sin ellos el caso se guarda sin vínculo).
    this.web.listar(CLIENTES).subscribe({ next: (lista) => this.clientes.set(lista), error: () => undefined });
    if (this.authService.hasPermission('web_catalogo.read')) {
      this.web.listar(PRODUCTOS).subscribe({ next: (lista) => this.productos.set(lista), error: () => undefined });
    }
  }

  protected id(item: WebCasoExitoResponse): string {
    return item.id_caso_exito;
  }

  protected nombre(item: WebCasoExitoResponse): string {
    return item.titulo;
  }

  protected textoBusqueda(item: WebCasoExitoResponse): string {
    return `${item.titulo} ${item.nombre_cliente ?? ''} ${item.nombre_producto ?? ''}`;
  }

  protected cargarFormulario(item: WebCasoExitoResponse | null): void {
    this.form.reset({
      titulo: item?.titulo ?? '',
      slug: item?.slug ?? '',
      resumen: item?.resumen ?? '',
      contenido: item?.contenido ?? '',
      url_externa: item?.url_externa ?? '',
      fecha_caso: item?.fecha_caso ?? '',
      id_cliente_referencia: item?.id_cliente_referencia ?? '',
      id_producto: item?.id_producto ?? '',
      orden: item?.orden ?? this.items().length,
      destacado: item?.destacado ?? false,
      publicado: item?.publicado ?? false,
    });
    this.metricas.set((item?.metricas ?? []).map((metrica) => ({ ...metrica })));
  }

  protected construirRequest(): WebCasoExitoRequest {
    const valor = this.form.getRawValue();
    return {
      titulo: valor.titulo.trim(),
      slug: this.texto(valor.slug),
      resumen: this.texto(valor.resumen),
      contenido: this.texto(valor.contenido),
      url_externa: this.texto(valor.url_externa),
      fecha_caso: this.texto(valor.fecha_caso),
      metricas: this.metricas()
        .map((metrica) => ({ valor: metrica.valor.trim(), etiqueta: metrica.etiqueta.trim() }))
        .filter((metrica) => metrica.valor !== '' && metrica.etiqueta !== ''),
      id_cliente_referencia: valor.id_cliente_referencia || null,
      id_producto: valor.id_producto || null,
      orden: valor.orden,
      destacado: valor.destacado,
      publicado: valor.publicado,
    };
  }

  protected agregarMetrica(): void {
    this.metricas.update((lista) => [...lista, { valor: '', etiqueta: '' }]);
  }

  protected editarMetrica(indice: number, campo: keyof MetricaCaso, valor: string): void {
    this.metricas.update((lista) => lista.map((metrica, i) => (i === indice ? { ...metrica, [campo]: valor } : metrica)));
  }

  protected quitarMetrica(indice: number): void {
    this.metricas.update((lista) => lista.filter((_, i) => i !== indice));
  }
}
