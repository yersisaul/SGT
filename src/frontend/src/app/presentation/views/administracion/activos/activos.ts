import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucidePencil, LucidePlus, LucideSearch, LucideTrash } from '@lucide/angular';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { ActivoResponse } from '../../../../core/models/activo.model';
import { EspecialidadCatalogo } from '../../../../core/models/catalogo.model';
import { ActivoService } from '../../../../core/services/activo.service';
import { ArchivoService } from '../../../../core/services/archivo.service';
import { CatalogoService } from '../../../../core/services/catalogo.service';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../../shared/utils/api-error.util';
import { ActivoFormSubmit, Formulario } from './components/formulario/formulario';

type DialogMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-activos',
  imports: [FormsModule, Button, Dialog, Spinner, Formulario, LucidePlus, LucideSearch, LucidePencil, LucideTrash, LucideCircleAlert],
  templateUrl: './activos.html',
  styleUrl: './activos.css',
})
export class Activos {
  private readonly authService = inject(AuthService);
  private readonly activoService = inject(ActivoService);
  private readonly archivoService = inject(ArchivoService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly notifications = inject(NotificationService);

  protected readonly canCreate = this.authService.hasPermission('activo.create');
  protected readonly canUpdate = this.authService.hasPermission('activo.update');
  protected readonly canDelete = this.authService.hasPermission('activo.delete');

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  private readonly activos = signal<ActivoResponse[]>([]);
  protected readonly especialidades = signal<EspecialidadCatalogo[]>([]);

  protected readonly searchTerm = signal('');

  protected readonly filtrados = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return this.activos();
    return this.activos().filter(
      (item) =>
        item.codigo.toLowerCase().includes(term) ||
        item.nombre.toLowerCase().includes(term) ||
        (item.ubicacion ?? '').toLowerCase().includes(term),
    );
  });

  protected readonly dialogMode = signal<DialogMode>(null);
  protected readonly dialogTarget = signal<ActivoResponse | null>(null);
  protected readonly formSubmitting = signal(false);

  protected readonly confirmTarget = signal<ActivoResponse | null>(null);
  protected readonly deleteSubmitting = signal(false);
  protected readonly deleteError = signal<string | null>(null);

  constructor() {
    this.loadAll();
  }

  protected reload(): void {
    this.loadAll();
  }

  protected nombreEspecialidad(id: string): string {
    return this.especialidades().find((item) => item.id_especialidad === id)?.nombre ?? '—';
  }

  protected openCreate(): void {
    this.dialogMode.set('create');
    this.dialogTarget.set(null);
  }

  protected openEdit(activo: ActivoResponse): void {
    if (!this.canUpdate) return;
    this.dialogMode.set('edit');
    this.dialogTarget.set(activo);
  }

  protected closeDialog(): void {
    this.dialogMode.set(null);
    this.dialogTarget.set(null);
  }

  protected handleFormSubmit(submission: ActivoFormSubmit): void {
    this.formSubmitting.set(true);
    const { request, archivo, eliminarArchivo } = submission;
    const esCreacion = this.dialogMode() === 'create';

    const guardar$ = esCreacion
      ? this.activoService.crear(request)
      : this.activoService.editar(this.dialogTarget()!.id_activo, request);

    guardar$.subscribe({
      next: (entidad) => this.gestionarArchivoYFinalizar(entidad, archivo, eliminarArchivo, esCreacion),
      error: () => this.formSubmitting.set(false),
    });
  }

  private gestionarArchivoYFinalizar(
    entidad: ActivoResponse,
    archivo: File | null,
    eliminarArchivo: boolean,
    esCreacion: boolean,
  ): void {
    const finalizar = (actualizado: ActivoResponse) => {
      this.activos.update((lista) =>
        esCreacion
          ? [...lista, actualizado]
          : lista.map((a) => (a.id_activo === actualizado.id_activo ? actualizado : a)),
      );
      this.formSubmitting.set(false);
      this.closeDialog();
      this.notifications.success(esCreacion ? 'Activo creado correctamente.' : 'Activo actualizado correctamente.');
    };

    if (archivo) {
      this.archivoService.subir<ActivoResponse>('activos', entidad.id_activo, archivo).subscribe({
        next: finalizar,
        error: () => {
          this.notifications.error('El activo se guardó, pero no se pudo subir la imagen.');
          finalizar(entidad);
        },
      });
      return;
    }

    if (eliminarArchivo) {
      this.archivoService.eliminar<ActivoResponse>('activos', entidad.id_activo).subscribe({
        next: finalizar,
        error: () => finalizar(entidad),
      });
      return;
    }

    finalizar(entidad);
  }

  protected requestDelete(activo: ActivoResponse): void {
    this.confirmTarget.set(activo);
    this.deleteError.set(null);
  }

  protected cancelDelete(): void {
    if (this.deleteSubmitting()) return;
    this.confirmTarget.set(null);
    this.deleteError.set(null);
  }

  protected confirmDeleteAction(): void {
    const target = this.confirmTarget();
    if (!target) return;

    this.deleteSubmitting.set(true);
    this.deleteError.set(null);
    this.activoService.eliminar(target.id_activo).subscribe({
      next: () => {
        this.activos.update((lista) => lista.filter((a) => a.id_activo !== target.id_activo));
        this.deleteSubmitting.set(false);
        this.confirmTarget.set(null);
        this.notifications.success('Activo eliminado correctamente.');
      },
      error: (error: unknown) => {
        this.deleteSubmitting.set(false);
        this.deleteError.set(extractApiErrorMessage(error));
      },
    });
  }

  private loadAll(): void {
    this.loading.set(true);
    this.loadError.set(null);
    forkJoin({
      activos: this.activoService.listar(),
      especialidades: this.catalogoService.getEspecialidades(),
    }).subscribe({
      next: ({ activos, especialidades }) => {
        this.activos.set(activos);
        this.especialidades.set(especialidades);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudieron cargar los activos.');
        this.loading.set(false);
      },
    });
  }
}
