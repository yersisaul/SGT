import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, of } from 'rxjs';

import {
  TIPO_DOCUMENTO_LABEL,
  TipoDocumentoWeb,
  WebDocumentoRequest,
  WebDocumentoResponse,
  WebProductoResponse,
} from '../../../../core/models/web.model';
import { ArchivoService } from '../../../../core/services/archivo.service';
import { DOCUMENTOS, PRODUCTOS } from '../../../../core/services/web-contenido.service';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { FileUpload } from '../../../../shared/components/file-upload/file-upload';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import {
  LucideCircleAlert,
  LucideEye,
  LucideFileText,
  LucidePencil,
  LucidePlus,
  LucideTrash,
} from '../../../../shared/icons/lucide-icons';
import { PublicadoSwitch } from '../components/publicado-switch';
import { WebToolbar } from '../components/web-toolbar';
import { SeccionWebBase } from '../seccion-web.base';

/** Tamaño máximo de PDF (WEB_DOCUMENTO_MAX_SIZE_MB del backend). */
const MAX_PDF_MB = 20;

/** Brochures, fichas técnicas y presentaciones en PDF: /api/web/documentos, permisos web_documento.*. */
@Component({
  selector: 'app-web-documentos',
  imports: [
    ReactiveFormsModule,
    Button,
    Dialog,
    FileUpload,
    Spinner,
    PublicadoSwitch,
    WebToolbar,
    LucideCircleAlert,
    LucideEye,
    LucideFileText,
    LucidePencil,
    LucidePlus,
    LucideTrash,
  ],
  templateUrl: './documentos.html',
  styleUrls: ['../../administracion/administracion.css', '../components/web-controles.css', '../web.css'],
})
export class Documentos extends SeccionWebBase<WebDocumentoResponse, WebDocumentoRequest> implements OnInit {
  private readonly archivoService = inject(ArchivoService);

  protected readonly recurso = DOCUMENTOS;
  protected readonly grupoPermiso = 'web_documento';
  protected readonly entidad = 'documento';
  protected readonly maxPdfMb = MAX_PDF_MB;
  protected readonly tipoLabel = TIPO_DOCUMENTO_LABEL;
  protected readonly tipos = Object.keys(TIPO_DOCUMENTO_LABEL) as TipoDocumentoWeb[];
  protected readonly productos = signal<WebProductoResponse[]>([]);

  /** El backend no publica un documento sin PDF: se publica después de subirlo. */
  private publicarTrasSubir = false;

  protected readonly form = new FormGroup({
    titulo: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    descripcion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    tipo: new FormControl<TipoDocumentoWeb>('BROCHURE', { nonNullable: true }),
    id_producto: new FormControl('', { nonNullable: true }),
    orden: new FormControl(0, { nonNullable: true, validators: [Validators.min(0), Validators.max(9999)] }),
    publicado: new FormControl(false, { nonNullable: true }),
  });

  override ngOnInit(): void {
    super.ngOnInit();
    if (this.authService.hasPermission('web_catalogo.read')) {
      this.web.listar(PRODUCTOS).subscribe({ next: (lista) => this.productos.set(lista), error: () => undefined });
    }
  }

  protected id(item: WebDocumentoResponse): string {
    return item.id_documento;
  }

  protected nombre(item: WebDocumentoResponse): string {
    return item.titulo;
  }

  protected textoBusqueda(item: WebDocumentoResponse): string {
    return `${item.titulo} ${item.nombre_producto ?? ''} ${item.nombre_original ?? ''}`;
  }

  protected cargarFormulario(item: WebDocumentoResponse | null): void {
    this.form.reset({
      titulo: item?.titulo ?? '',
      descripcion: item?.descripcion ?? '',
      tipo: item?.tipo ?? 'BROCHURE',
      id_producto: item?.id_producto ?? '',
      orden: item?.orden ?? this.items().length,
      publicado: item?.publicado ?? false,
    });
  }

  protected construirRequest(): WebDocumentoRequest {
    const valor = this.form.getRawValue();
    const tienePdf = !!this.seleccionado()?.url_archivo;
    this.publicarTrasSubir = valor.publicado && !tienePdf;
    return {
      titulo: valor.titulo.trim(),
      descripcion: this.texto(valor.descripcion),
      tipo: valor.tipo,
      id_producto: valor.id_producto || null,
      orden: valor.orden,
      publicado: valor.publicado && tienePdf,
    };
  }

  protected override trasGuardarArchivo(guardado: WebDocumentoResponse, subioArchivo: boolean): Observable<WebDocumentoResponse> {
    if (this.publicarTrasSubir && subioArchivo) {
      return this.web.cambiarPublicacion(this.recurso, guardado.id_documento, true);
    }
    if (this.publicarTrasSubir) {
      this.notifications.show('El documento quedó oculto: súbele un PDF para poder publicarlo.');
    }
    return of(guardado);
  }

  protected verPdf(item: WebDocumentoResponse): void {
    if (!item.url_archivo) return;
    this.archivoService.descargarBlob(item.url_archivo).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    });
  }

  protected tamano(bytes: number | null): string {
    if (!bytes) return '';
    return bytes < 1024 * 1024 ? `${Math.round(bytes / 1024)} KB` : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
