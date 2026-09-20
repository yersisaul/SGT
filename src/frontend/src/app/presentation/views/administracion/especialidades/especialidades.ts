import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucidePencil, LucidePlus, LucideSearch, LucideTrash } from '@lucide/angular';

import { AuthService } from '../../../../core/auth/auth.service';
import { EspecialidadRequest, EspecialidadResponse } from '../../../../core/models/especialidad.model';
import { EspecialidadService } from '../../../../core/services/especialidad.service';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../../shared/utils/api-error.util';
import { Formulario } from './components/formulario/formulario';

type DialogMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-especialidades',
  imports: [FormsModule, Button, Dialog, Spinner, Formulario, LucidePlus, LucideSearch, LucidePencil, LucideTrash, LucideCircleAlert],
  templateUrl: './especialidades.html',
  styleUrl: './especialidades.css',
})
export class Especialidades {
  private readonly authService = inject(AuthService);
  private readonly especialidadService = inject(EspecialidadService);
  private readonly notifications = inject(NotificationService);

  protected readonly canCreate = this.authService.hasPermission('especialidad.create');
  protected readonly canUpdate = this.authService.hasPermission('especialidad.update');
  protected readonly canDelete = this.authService.hasPermission('especialidad.delete');

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  private readonly especialidades = signal<EspecialidadResponse[]>([]);

  protected readonly searchTerm = signal('');

  protected readonly filtradas = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return this.especialidades();
    return this.especialidades().filter(
      (item) => item.nombre.toLowerCase().includes(term) || (item.descripcion ?? '').toLowerCase().includes(term),
    );
  });

  protected readonly dialogMode = signal<DialogMode>(null);
  protected readonly dialogTarget = signal<EspecialidadResponse | null>(null);
  protected readonly formSubmitting = signal(false);

  protected readonly confirmTarget = signal<EspecialidadResponse | null>(null);
  protected readonly deleteSubmitting = signal(false);
  protected readonly deleteError = signal<string | null>(null);

  constructor() {
    this.loadAll();
  }

  protected reload(): void {
    this.loadAll();
  }

  protected openCreate(): void {
    this.dialogMode.set('create');
    this.dialogTarget.set(null);
  }

  protected openEdit(especialidad: EspecialidadResponse): void {
    if (!this.canUpdate) return;
    this.dialogMode.set('edit');
    this.dialogTarget.set(especialidad);
  }

  protected closeDialog(): void {
    this.dialogMode.set(null);
    this.dialogTarget.set(null);
  }

  protected handleFormSubmit(request: EspecialidadRequest): void {
    this.formSubmitting.set(true);

    if (this.dialogMode() === 'create') {
      this.especialidadService.crear(request).subscribe({
        next: (creada) => {
          this.especialidades.update((lista) => [...lista, creada]);
          this.formSubmitting.set(false);
          this.closeDialog();
          this.notifications.success('Especialidad creada correctamente.');
        },
        error: () => this.formSubmitting.set(false),
      });
      return;
    }

    const target = this.dialogTarget();
    if (this.dialogMode() === 'edit' && target) {
      this.especialidadService.editar(target.id_especialidad, request).subscribe({
        next: (actualizada) => {
          this.especialidades.update((lista) =>
            lista.map((e) => (e.id_especialidad === actualizada.id_especialidad ? actualizada : e)),
          );
          this.formSubmitting.set(false);
          this.closeDialog();
          this.notifications.success('Especialidad actualizada correctamente.');
        },
        error: () => this.formSubmitting.set(false),
      });
    }
  }

  protected requestDelete(especialidad: EspecialidadResponse): void {
    this.confirmTarget.set(especialidad);
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
    this.especialidadService.eliminar(target.id_especialidad).subscribe({
      next: () => {
        this.especialidades.update((lista) => lista.filter((e) => e.id_especialidad !== target.id_especialidad));
        this.deleteSubmitting.set(false);
        this.confirmTarget.set(null);
        this.notifications.success('Especialidad eliminada correctamente.');
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
    this.especialidadService.listar().subscribe({
      next: (lista) => {
        this.especialidades.set(lista);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudieron cargar las especialidades.');
        this.loading.set(false);
      },
    });
  }
}
