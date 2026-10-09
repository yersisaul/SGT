import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucidePlus, LucideSearch, LucideTrash } from '../../../../shared/icons/lucide-icons';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { PermisoResponse } from '../../../../core/models/permiso.model';
import { RolPermisoResponse } from '../../../../core/models/rol-permiso.model';
import { RolRequest, RolResponse } from '../../../../core/models/rol.model';
import { PermisoService } from '../../../../core/services/permiso.service';
import { RolPermisoService } from '../../../../core/services/rol-permiso.service';
import { RolService } from '../../../../core/services/rol.service';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../../shared/utils/api-error.util';
import { Formulario } from './components/formulario/formulario';
import { Permisos } from './components/permisos/permisos';

type DialogMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-roles',
  imports: [FormsModule, Button, Dialog, Spinner, Formulario, Permisos, LucidePlus, LucideSearch, LucideTrash, LucideCircleAlert],
  templateUrl: './roles.html',
  styleUrl: './roles.css',
})
export class Roles {
  private readonly authService = inject(AuthService);
  private readonly rolService = inject(RolService);
  private readonly permisoService = inject(PermisoService);
  private readonly rolPermisoService = inject(RolPermisoService);
  private readonly notifications = inject(NotificationService);

  protected readonly canCreate = this.authService.hasPermission('rol.create');
  protected readonly canUpdate = this.authService.hasPermission('rol.update');
  protected readonly canDelete = this.authService.hasPermission('rol.delete');
  protected readonly canReadPermisos = this.authService.hasPermission('permiso.read');
  protected readonly canReadRolPermiso = this.authService.hasPermission('rolpermiso.read');
  protected readonly canManagePermisos =
    this.authService.hasPermission('rolpermiso.create') && this.authService.hasPermission('rolpermiso.delete');

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  private readonly roles = signal<RolResponse[]>([]);
  protected readonly permisosCatalogo = signal<PermisoResponse[]>([]);
  protected readonly rolPermisos = signal<RolPermisoResponse[]>([]);

  protected readonly searchTerm = signal('');

  protected readonly filtrados = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return this.roles();
    return this.roles().filter(
      (item) => item.nombre.toLowerCase().includes(term) || (item.descripcion ?? '').toLowerCase().includes(term),
    );
  });

  protected readonly dialogMode = signal<DialogMode>(null);
  protected readonly dialogTarget = signal<RolResponse | null>(null);
  protected readonly formSubmitting = signal(false);

  protected readonly confirmTarget = signal<RolResponse | null>(null);
  protected readonly deleteSubmitting = signal(false);
  protected readonly deleteError = signal<string | null>(null);

  protected readonly permisosDelRolActual = computed<RolPermisoResponse[]>(() => {
    const target = this.dialogTarget();
    if (!target) return [];
    return this.rolPermisos().filter((rp) => rp.id_rol === target.id_rol);
  });

  constructor() {
    this.loadAll();
  }

  protected reload(): void {
    this.loadAll();
  }

  protected contarPermisos(idRol: string): number {
    return this.rolPermisos().filter((rp) => rp.id_rol === idRol).length;
  }

  protected openCreate(): void {
    this.dialogMode.set('create');
    this.dialogTarget.set(null);
  }

  protected openEdit(rol: RolResponse): void {
    this.dialogMode.set('edit');
    this.dialogTarget.set(rol);
  }

  protected closeDialog(): void {
    this.dialogMode.set(null);
    this.dialogTarget.set(null);
  }

  protected handleFormSubmit(request: RolRequest): void {
    this.formSubmitting.set(true);

    if (this.dialogMode() === 'create') {
      this.rolService.crear(request).subscribe({
        next: (creado) => {
          this.roles.update((lista) => [...lista, creado]);
          this.formSubmitting.set(false);
          this.closeDialog();
          this.notifications.success('Rol creado correctamente. Ahora puedes asignarle permisos.');
        },
        error: () => this.formSubmitting.set(false),
      });
      return;
    }

    const target = this.dialogTarget();
    if (this.dialogMode() === 'edit' && target) {
      this.rolService.editar(target.id_rol, request).subscribe({
        next: (actualizado) => {
          this.roles.update((lista) => lista.map((r) => (r.id_rol === actualizado.id_rol ? actualizado : r)));
          this.dialogTarget.set(actualizado);
          this.formSubmitting.set(false);
          this.notifications.success('Rol actualizado correctamente.');
        },
        error: () => this.formSubmitting.set(false),
      });
    }
  }

  protected handlePermisoAsignado(asignacion: RolPermisoResponse): void {
    this.rolPermisos.update((lista) => [...lista, asignacion]);
  }

  protected handlePermisoRevocado(idRolPermiso: string): void {
    this.rolPermisos.update((lista) => lista.filter((rp) => rp.id_rol_permiso !== idRolPermiso));
  }

  protected requestDelete(rol: RolResponse): void {
    this.confirmTarget.set(rol);
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
    this.rolService.eliminar(target.id_rol).subscribe({
      next: () => {
        this.roles.update((lista) => lista.filter((r) => r.id_rol !== target.id_rol));
        this.deleteSubmitting.set(false);
        this.confirmTarget.set(null);
        if (this.dialogTarget()?.id_rol === target.id_rol) {
          this.closeDialog();
        }
        this.notifications.success('Rol eliminado correctamente.');
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
      roles: this.rolService.listar(),
      permisos: this.canReadPermisos ? this.permisoService.listar() : Promise.resolve([]),
      rolPermisos: this.canReadRolPermiso ? this.rolPermisoService.listar() : Promise.resolve([]),
    }).subscribe({
      next: ({ roles, permisos, rolPermisos }) => {
        this.roles.set(roles);
        this.permisosCatalogo.set(permisos);
        this.rolPermisos.set(rolPermisos);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudieron cargar los roles.');
        this.loading.set(false);
      },
    });
  }
}
