import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucidePencil, LucidePlus, LucideSearch, LucideTrash } from '../../../../shared/icons/lucide-icons';

import { AuthService } from '../../../../core/auth/auth.service';
import { EstadoRequest, EstadoResponse, esEstadoCritico } from '../../../../core/models/estado.model';
import { EstadoService } from '../../../../core/services/estado.service';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../../shared/utils/api-error.util';
import { Formulario } from './components/formulario/formulario';

type DialogMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-estados',
  imports: [FormsModule, Button, Dialog, Spinner, Formulario, LucidePlus, LucideSearch, LucidePencil, LucideTrash, LucideCircleAlert],
  templateUrl: './estados.html',
  styleUrl: './estados.css',
})
export class Estados {
  private readonly authService = inject(AuthService);
  private readonly estadoService = inject(EstadoService);
  private readonly notifications = inject(NotificationService);

  protected readonly canCreate = this.authService.hasPermission('estado.create');
  protected readonly canUpdate = this.authService.hasPermission('estado.update');
  protected readonly canDelete = this.authService.hasPermission('estado.delete');

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  private readonly estados = signal<EstadoResponse[]>([]);

  protected readonly searchTerm = signal('');
  protected readonly esEstadoCritico = esEstadoCritico;

  protected readonly filtrados = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return this.estados();
    return this.estados().filter((item) => item.nombre.toLowerCase().includes(term));
  });

  protected readonly dialogMode = signal<DialogMode>(null);
  protected readonly dialogTarget = signal<EstadoResponse | null>(null);
  protected readonly formSubmitting = signal(false);

  protected readonly confirmTarget = signal<EstadoResponse | null>(null);
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

  protected openEdit(estado: EstadoResponse): void {
    if (!this.canUpdate) return;
    this.dialogMode.set('edit');
    this.dialogTarget.set(estado);
  }

  protected closeDialog(): void {
    this.dialogMode.set(null);
    this.dialogTarget.set(null);
  }

  protected handleFormSubmit(request: EstadoRequest): void {
    this.formSubmitting.set(true);

    if (this.dialogMode() === 'create') {
      this.estadoService.crear(request).subscribe({
        next: (creado) => {
          this.estados.update((lista) => [...lista, creado]);
          this.formSubmitting.set(false);
          this.closeDialog();
          this.notifications.success('Estado creado correctamente.');
        },
        error: () => this.formSubmitting.set(false),
      });
      return;
    }

    const target = this.dialogTarget();
    if (this.dialogMode() === 'edit' && target) {
      this.estadoService.editar(target.id_estado, request).subscribe({
        next: (actualizado) => {
          this.estados.update((lista) => lista.map((e) => (e.id_estado === actualizado.id_estado ? actualizado : e)));
          this.formSubmitting.set(false);
          this.closeDialog();
          this.notifications.success('Estado actualizado correctamente.');
        },
        error: () => this.formSubmitting.set(false),
      });
    }
  }

  protected requestDelete(estado: EstadoResponse): void {
    this.confirmTarget.set(estado);
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
    this.estadoService.eliminar(target.id_estado).subscribe({
      next: () => {
        this.estados.update((lista) => lista.filter((e) => e.id_estado !== target.id_estado));
        this.deleteSubmitting.set(false);
        this.confirmTarget.set(null);
        this.notifications.success('Estado eliminado correctamente.');
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
    this.estadoService.listar().subscribe({
      next: (lista) => {
        this.estados.set(lista);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudieron cargar los estados.');
        this.loading.set(false);
      },
    });
  }
}
