import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideCircleAlert, LucideSearch } from '../../../../shared/icons/lucide-icons';

import { esLecturaBase } from '../../../../core/auth/permisos-base';
import { PermisoResponse } from '../../../../core/models/permiso.model';
import { PermisoService } from '../../../../core/services/permiso.service';
import { Badge } from '../../../../shared/components/badge/badge';
import { Button } from '../../../../shared/components/button/button';
import { Spinner } from '../../../../shared/components/spinner/spinner';

/**
 * Catálogo de permisos en solo lectura (observación 2026-10-09): los códigos
 * los define el backend (@PreAuthorize + DataSeeder); crear, editar o borrar
 * uno desde la interfaz no tiene efecto o deja roles inconsistentes. Se
 * asignan a los Roles desde la vista de Roles.
 */
@Component({
  selector: 'app-permisos',
  imports: [FormsModule, Badge, Button, Spinner, LucideSearch, LucideCircleAlert],
  templateUrl: './permisos.html',
  styleUrl: './permisos.css',
})
export class Permisos {
  private readonly permisoService = inject(PermisoService);

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

  protected readonly esLecturaBase = esLecturaBase;

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
