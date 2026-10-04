import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucidePencil, LucidePlus, LucideSearch, LucideTrash, LucideUsers } from '@lucide/angular';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { UsuarioCatalogo } from '../../../../core/models/catalogo.model';
import { EspecialidadRequest, EspecialidadResponse } from '../../../../core/models/especialidad.model';
import { CatalogoService } from '../../../../core/services/catalogo.service';
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
  imports: [
    FormsModule,
    Button,
    Dialog,
    Spinner,
    Formulario,
    LucidePlus,
    LucideSearch,
    LucidePencil,
    LucideTrash,
    LucideCircleAlert,
    LucideUsers,
  ],
  templateUrl: './especialidades.html',
  styleUrl: './especialidades.css',
})
export class Especialidades {
  private readonly authService = inject(AuthService);
  private readonly especialidadService = inject(EspecialidadService);
  private readonly notifications = inject(NotificationService);
  private readonly catalogoService = inject(CatalogoService);

  protected readonly canCreate = this.authService.hasPermission('especialidad.create');
  protected readonly canUpdate = this.authService.hasPermission('especialidad.update');
  protected readonly canDelete = this.authService.hasPermission('especialidad.delete');
  protected readonly canGestionarEquipo = this.authService.hasPermission('especialidad.gestionar_equipo');

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

  // ---- Equipo de la especialidad (PRD FR-048): miembros y responsables ----
  protected readonly equipoTarget = signal<EspecialidadResponse | null>(null);
  protected readonly equipoCandidatos = signal<UsuarioCatalogo[]>([]);
  /** id_usuario → es_responsable; si no está en el mapa, no es miembro. */
  protected readonly equipoSeleccion = signal<Map<string, boolean>>(new Map());
  protected readonly equipoLoading = signal(false);
  protected readonly equipoSubmitting = signal(false);
  protected readonly equipoError = signal<string | null>(null);

  constructor() {
    this.loadAll();
  }

  protected openEquipo(especialidad: EspecialidadResponse): void {
    this.equipoTarget.set(especialidad);
    this.equipoError.set(null);
    this.equipoLoading.set(true);
    forkJoin({
      candidatos: this.catalogoService.getUsuariosEjecutores(),
      miembros: this.especialidadService.miembros(especialidad.id_especialidad),
    }).subscribe({
      next: ({ candidatos, miembros }) => {
        this.equipoCandidatos.set(candidatos);
        this.equipoSeleccion.set(new Map(miembros.map((m) => [m.id_usuario, m.es_responsable])));
        this.equipoLoading.set(false);
      },
      error: (error: unknown) => {
        this.equipoError.set(extractApiErrorMessage(error));
        this.equipoLoading.set(false);
      },
    });
  }

  protected closeEquipo(): void {
    if (this.equipoSubmitting()) return;
    this.equipoTarget.set(null);
  }

  protected esMiembro(idUsuario: string): boolean {
    return this.equipoSeleccion().has(idUsuario);
  }

  protected esResponsable(idUsuario: string): boolean {
    return this.equipoSeleccion().get(idUsuario) === true;
  }

  protected toggleMiembro(idUsuario: string, miembro: boolean): void {
    this.equipoSeleccion.update((actual) => {
      const copia = new Map(actual);
      if (miembro) {
        copia.set(idUsuario, copia.get(idUsuario) ?? false);
      } else {
        copia.delete(idUsuario);
      }
      return copia;
    });
  }

  protected toggleResponsable(idUsuario: string, responsable: boolean): void {
    this.equipoSeleccion.update((actual) => new Map(actual).set(idUsuario, responsable));
  }

  protected guardarEquipo(): void {
    const target = this.equipoTarget();
    if (!target) return;
    this.equipoSubmitting.set(true);
    this.equipoError.set(null);
    const miembros = [...this.equipoSeleccion()].map(([id_usuario, es_responsable]) => ({ id_usuario, es_responsable }));
    this.especialidadService.guardarMiembros(target.id_especialidad, { miembros }).subscribe({
      next: () => {
        this.equipoSubmitting.set(false);
        this.equipoTarget.set(null);
        this.notifications.success(`Equipo de ${target.nombre} actualizado.`);
      },
      error: (error: unknown) => {
        this.equipoSubmitting.set(false);
        this.equipoError.set(extractApiErrorMessage(error));
      },
    });
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
