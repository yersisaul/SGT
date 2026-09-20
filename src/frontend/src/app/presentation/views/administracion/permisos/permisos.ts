import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucidePencil, LucidePlus, LucideSearch, LucideTrash } from '@lucide/angular';

import { AuthService } from '../../../../core/auth/auth.service';
import { PermisoRequest, PermisoResponse } from '../../../../core/models/permiso.model';
import { PermisoService } from '../../../../core/services/permiso.service';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../../shared/utils/api-error.util';
import { Formulario } from './components/formulario/formulario';

type DialogMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-permisos',
  imports: [FormsModule, Button, Dialog, Spinner, Formulario, LucidePlus, LucideSearch, LucidePencil, LucideTrash, LucideCircleAlert],
  templateUrl: './permisos.html',
  styleUrl: './permisos.css',
})
export class Permisos {
  private readonly authService = inject(AuthService);
  private readonly permisoService = inject(PermisoService);
  private readonly notifications = inject(NotificationService);

  protected readonly canCreate = this.authService.hasPermission('permiso.create');
  protected readonly canUpdate = this.authService.hasPermission('permiso.update');
  protected readonly canDelete = this.authService.hasPermission('permiso.delete');

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  private readonly permisos = signal<PermisoResponse[]>([]);

  protected readonly searchTerm = signal('');

  protected readonly filtrados = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return this.permisos();
    return this.permisos().filter(
      (item) => item.codigo.toLowerCase().includes(term) || (item.descripcion ?? '').toLowerCase().includes(term),
    );
  });

  protected readonly dialogMode = signal<DialogMode>(null);
  protected readonly dialogTarget = signal<PermisoResponse | null>(null);
  protected readonly formSubmitting = signal(false);

  protected readonly confirmTarget = signal<PermisoResponse | null>(null);
  protected readonly deleteSubmitting = signal(false);
  protected readonly deleteError = signal<string | null>(null);

  constructor() {
    this.loadAll();
  }

  protected reload(): void {
    this.loadAll();
  }

  protected recurso(codigo: string): string {
    return codigo.split('.')[0] ?? codigo;
  }

  protected accion(codigo: string): string {
    return codigo.split('.')[1] ?? '—';
  }

  protected openCreate(): void {
    this.dialogMode.set('create');
    this.dialogTarget.set(null);
  }

  protected openEdit(permiso: PermisoResponse): void {
    if (!this.canUpdate) return;
    this.dialogMode.set('edit');
    this.dialogTarget.set(permiso);
  }

  protected closeDialog(): void {
    this.dialogMode.set(null);
    this.dialogTarget.set(null);
  }

  protected handleFormSubmit(request: PermisoRequest): void {
    this.formSubmitting.set(true);

    if (this.dialogMode() === 'create') {
      this.permisoService.crear(request).subscribe({
        next: (creado) => {
          this.permisos.update((lista) => [...lista, creado]);
          this.formSubmitting.set(false);
          this.closeDialog();
          this.notifications.success('Permiso creado correctamente.');
        },
        error: () => this.formSubmitting.set(false),
      });
      return;
    }

    const target = this.dialogTarget();
    if (this.dialogMode() === 'edit' && target) {
      this.permisoService.editar(target.id_permiso, request).subscribe({
        next: (actualizado) => {
          this.permisos.update((lista) =>
            lista.map((p) => (p.id_permiso === actualizado.id_permiso ? actualizado : p)),
          );
          this.formSubmitting.set(false);
          this.closeDialog();
          this.notifications.success('Permiso actualizado correctamente.');
        },
        error: () => this.formSubmitting.set(false),
      });
    }
  }

  protected requestDelete(permiso: PermisoResponse): void {
    this.confirmTarget.set(permiso);
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
    this.permisoService.eliminar(target.id_permiso).subscribe({
      next: () => {
        this.permisos.update((lista) => lista.filter((p) => p.id_permiso !== target.id_permiso));
        this.deleteSubmitting.set(false);
        this.confirmTarget.set(null);
        this.notifications.success('Permiso eliminado correctamente.');
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
    this.permisoService.listar().subscribe({
      next: (lista) => {
        this.permisos.set([...lista].sort((a, b) => a.codigo.localeCompare(b.codigo)));
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudieron cargar los permisos.');
        this.loading.set(false);
      },
    });
  }
}
