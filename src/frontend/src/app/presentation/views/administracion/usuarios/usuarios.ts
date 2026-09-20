import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucidePencil, LucidePlus, LucideSearch, LucideTrash } from '@lucide/angular';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { RolCatalogo } from '../../../../core/models/catalogo.model';
import { UsuarioRequest, UsuarioResponse } from '../../../../core/models/usuario.model';
import { CatalogoService } from '../../../../core/services/catalogo.service';
import { UsuarioService } from '../../../../core/services/usuario.service';
import { Badge } from '../../../../shared/components/badge/badge';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../../shared/utils/api-error.util';
import { Formulario } from './components/formulario/formulario';

type DialogMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-usuarios',
  imports: [FormsModule, Badge, Button, Dialog, Spinner, Formulario, LucidePlus, LucideSearch, LucidePencil, LucideTrash, LucideCircleAlert],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.css',
})
export class Usuarios {
  private readonly authService = inject(AuthService);
  private readonly usuarioService = inject(UsuarioService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly notifications = inject(NotificationService);

  protected readonly canCreate = this.authService.hasPermission('usuario.create');
  protected readonly canUpdate = this.authService.hasPermission('usuario.update');
  protected readonly canDelete = this.authService.hasPermission('usuario.delete');
  protected readonly currentUserId = computed(() => this.authService.user()?.id ?? null);

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  private readonly usuarios = signal<UsuarioResponse[]>([]);
  protected readonly roles = signal<RolCatalogo[]>([]);

  protected readonly searchTerm = signal('');

  protected readonly filtrados = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return this.usuarios();
    return this.usuarios().filter(
      (item) =>
        item.email.toLowerCase().includes(term) ||
        item.nombres.toLowerCase().includes(term) ||
        item.apellidos.toLowerCase().includes(term),
    );
  });

  protected readonly dialogMode = signal<DialogMode>(null);
  protected readonly dialogTarget = signal<UsuarioResponse | null>(null);
  protected readonly formSubmitting = signal(false);

  protected readonly confirmTarget = signal<UsuarioResponse | null>(null);
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

  protected openEdit(usuario: UsuarioResponse): void {
    if (!this.canUpdate) return;
    this.dialogMode.set('edit');
    this.dialogTarget.set(usuario);
  }

  protected closeDialog(): void {
    this.dialogMode.set(null);
    this.dialogTarget.set(null);
  }

  protected handleFormSubmit(request: UsuarioRequest): void {
    this.formSubmitting.set(true);

    if (this.dialogMode() === 'create') {
      this.usuarioService.crear(request).subscribe({
        next: (creado) => {
          this.usuarios.update((lista) => [...lista, creado]);
          this.formSubmitting.set(false);
          this.closeDialog();
          this.notifications.success('Usuario creado correctamente.');
        },
        error: () => this.formSubmitting.set(false),
      });
      return;
    }

    const target = this.dialogTarget();
    if (this.dialogMode() === 'edit' && target) {
      this.usuarioService.editar(target.id_usuario, request).subscribe({
        next: (actualizado) => {
          this.usuarios.update((lista) =>
            lista.map((u) => (u.id_usuario === actualizado.id_usuario ? actualizado : u)),
          );
          this.formSubmitting.set(false);
          this.closeDialog();
          this.notifications.success('Usuario actualizado correctamente.');
        },
        error: () => this.formSubmitting.set(false),
      });
    }
  }

  protected requestDelete(usuario: UsuarioResponse): void {
    if (usuario.id_usuario === this.currentUserId()) return;
    this.confirmTarget.set(usuario);
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
    this.usuarioService.eliminar(target.id_usuario).subscribe({
      next: () => {
        this.usuarios.update((lista) => lista.filter((u) => u.id_usuario !== target.id_usuario));
        this.deleteSubmitting.set(false);
        this.confirmTarget.set(null);
        this.notifications.success('Usuario eliminado correctamente.');
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
      usuarios: this.usuarioService.listar(),
      roles: this.catalogoService.getRoles(),
    }).subscribe({
      next: ({ usuarios, roles }) => {
        this.usuarios.set(usuarios);
        this.roles.set(roles);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudieron cargar los usuarios.');
        this.loading.set(false);
      },
    });
  }
}
