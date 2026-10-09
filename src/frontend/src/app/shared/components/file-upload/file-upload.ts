import { Component, DestroyRef, computed, effect, forwardRef, inject, input, output, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { LucideCircleAlert, LucidePaperclip, LucideUpload, LucideX } from '../../icons/lucide-icons';

import { ArchivoService } from '../../../core/services/archivo.service';
import { Spinner } from '../spinner/spinner';

let nextFileUploadId = 0;

const EXTENSIONES_PERMITIDAS: Record<'imagen' | 'adjunto', string[]> = {
  imagen: ['jpg', 'jpeg', 'png', 'webp'],
  adjunto: ['jpg', 'jpeg', 'png', 'webp', 'pdf'],
};

const ACCEPT_MIME: Record<'imagen' | 'adjunto', string> = {
  imagen: 'image/png,image/jpeg,image/webp',
  adjunto: 'image/png,image/jpeg,image/webp,application/pdf',
};

/**
 * Componente reutilizable de selección/drag&drop de un archivo (imagen o
 * adjunto), para los 6 formularios que antes pedían escribir una URL a mano
 * (Activo, Usuario, Solicitud, Requerimiento, Aprobacion, Orden).
 *
 * El valor del control (ControlValueAccessor) es el File recién seleccionado
 * (o null): este componente NUNCA sube el archivo por sí mismo. El flujo es
 * seleccionar -> preview local -> guardar formulario -> el contenedor sube
 * el archivo (vía ArchivoService) recién después de crear/editar la entidad,
 * cuando ya existe un id.
 *
 * `currentUrl` es la ruta de descarga que ya devuelve el backend
 * (ActivoResponse.url_img, etc., algo como "/api/archivos/activos/{id}") si
 * la entidad ya tiene un archivo guardado; se usa para mostrar "Archivo
 * actual" con su preview/Ver archivo, independiente de si el usuario
 * seleccionó uno nuevo.
 */
@Component({
  selector: 'app-file-upload',
  imports: [Spinner, LucideUpload, LucidePaperclip, LucideX, LucideCircleAlert],
  templateUrl: './file-upload.html',
  styleUrl: './file-upload.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FileUpload),
      multi: true,
    },
  ],
})
export class FileUpload implements ControlValueAccessor {
  private readonly archivoService = inject(ArchivoService);
  private readonly destroyRef = inject(DestroyRef);

  readonly label = input('Archivo');
  readonly tipo = input<'imagen' | 'adjunto'>('imagen');
  readonly maxSizeMb = input(5);
  readonly currentUrl = input<string | null>(null);
  readonly errorMessage = input<string | null>(null);

  /** El usuario pidió quitar el archivo ya guardado (el contenedor decide
   * cuándo llamar a ArchivoService.eliminar, normalmente al guardar). */
  readonly eliminarActual = output<void>();

  protected readonly inputId = `app-file-upload-${nextFileUploadId++}`;
  protected readonly accept = computed(() => ACCEPT_MIME[this.tipo()]);
  protected readonly esImagen = computed(() => this.tipo() === 'imagen');

  protected readonly selectedFile = signal<File | null>(null);
  protected readonly previewUrl = signal<string | null>(null);
  protected readonly currentPreviewUrl = signal<string | null>(null);
  protected readonly cargandoPreview = signal(false);
  protected readonly dragOver = signal(false);
  protected readonly localError = signal<string | null>(null);
  protected readonly disabled = signal(false);
  protected readonly actualEliminado = signal(false);

  private onChange: (value: File | null) => void = () => {};
  private onTouched: () => void = () => {};
  private ultimaUrlCargada: string | null = null;

  constructor() {
    effect(() => {
      const url = this.currentUrl();
      if (!url) {
        this.ultimaUrlCargada = null;
        this.revocar(this.currentPreviewUrl());
        this.currentPreviewUrl.set(null);
        return;
      }
      if (this.esImagen() && !this.selectedFile() && url !== this.ultimaUrlCargada) {
        this.ultimaUrlCargada = url;
        this.cargarPreviewActual(url);
      }
    });

    this.destroyRef.onDestroy(() => {
      this.revocar(this.previewUrl());
      this.revocar(this.currentPreviewUrl());
    });
  }

  writeValue(value: File | null): void {
    this.revocar(this.previewUrl());
    this.selectedFile.set(value ?? null);
    this.previewUrl.set(value && value.type.startsWith('image/') ? URL.createObjectURL(value) : null);
  }

  registerOnChange(fn: (value: File | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  protected onFileInputChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const file = target.files?.[0] ?? null;
    target.value = '';
    this.procesarArchivo(file);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    if (this.disabled()) return;
    this.procesarArchivo(event.dataTransfer?.files?.[0] ?? null);
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    if (!this.disabled()) this.dragOver.set(true);
  }

  protected onDragLeave(): void {
    this.dragOver.set(false);
  }

  protected quitarSeleccion(): void {
    this.writeValue(null);
    this.onChange(null);
    this.onTouched();
  }

  protected quitarActual(): void {
    this.revocar(this.currentPreviewUrl());
    this.currentPreviewUrl.set(null);
    this.ultimaUrlCargada = null;
    this.actualEliminado.set(true);
    this.eliminarActual.emit();
  }

  protected verArchivoActual(): void {
    const url = this.currentUrl();
    if (!url) return;
    this.archivoService.descargarBlob(url).subscribe((blob) => {
      const objectUrl = URL.createObjectURL(blob);
      window.open(objectUrl, '_blank');
      setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
    });
  }

  protected formatearTamano(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private procesarArchivo(file: File | null): void {
    if (!file || this.disabled()) return;

    const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
    const permitidas = EXTENSIONES_PERMITIDAS[this.tipo()];
    if (!permitidas.includes(extension)) {
      this.localError.set(`Formato no permitido. Usa: ${permitidas.join(', ')}.`);
      return;
    }
    const maxBytes = this.maxSizeMb() * 1024 * 1024;
    if (file.size > maxBytes) {
      this.localError.set(`El archivo supera el tamaño máximo permitido (${this.maxSizeMb()} MB).`);
      return;
    }

    this.localError.set(null);
    this.actualEliminado.set(false);
    this.writeValue(file);
    this.onChange(file);
    this.onTouched();
  }

  private cargarPreviewActual(url: string): void {
    this.cargandoPreview.set(true);
    this.archivoService.descargarBlob(url).subscribe({
      next: (blob) => {
        this.currentPreviewUrl.set(URL.createObjectURL(blob));
        this.cargandoPreview.set(false);
      },
      error: () => this.cargandoPreview.set(false),
    });
  }

  private revocar(url: string | null): void {
    if (url) URL.revokeObjectURL(url);
  }
}
