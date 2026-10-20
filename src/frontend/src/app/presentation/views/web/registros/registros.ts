import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { AuthService } from '../../../../core/auth/auth.service';
import { UsuarioResponse } from '../../../../core/models/usuario.model';
import { UsuarioService } from '../../../../core/services/usuario.service';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import { LucideCircleAlert, LucideSearch, LucideUserCheck } from '../../../../shared/icons/lucide-icons';
import { NotificationService } from '../../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../../shared/utils/api-error.util';

/**
 * Clientes que se registraron desde el formulario del sitio
 * (POST /api/auth/registro-cliente): nacen deshabilitados y no pueden
 * iniciar sesión en el SGT hasta que un Administrador los habilite.
 */
@Component({
  selector: 'app-web-registros',
  imports: [Button, Dialog, Spinner, LucideCircleAlert, LucideSearch, LucideUserCheck],
  templateUrl: './registros.html',
  styleUrls: ['../../administracion/administracion.css', '../components/web-controles.css', '../web.css'],
})
export class Registros implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly usuarioService = inject(UsuarioService);
  private readonly notifications = inject(NotificationService);

  protected readonly canDelete = this.authService.hasPermission('usuario.delete');

  protected readonly pendientes = signal<UsuarioResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly busqueda = signal('');
  protected readonly procesando = signal<string | null>(null);

  protected readonly aRechazar = signal<UsuarioResponse | null>(null);
  protected readonly rechazando = signal(false);
  protected readonly errorRechazo = signal<string | null>(null);

  protected readonly filtrados = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    return this.pendientes().filter(
      (u) => !termino || `${u.nombres} ${u.apellidos} ${u.email}`.toLowerCase().includes(termino),
    );
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.usuarioService.listarPorHabilitado(false).subscribe({
      next: (usuarios) => {
        // Solo cuentas de Cliente: un empleado deshabilitado no es un registro web.
        this.pendientes.set(usuarios.filter((u) => u.nombre_rol?.toLowerCase() === 'cliente'));
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudieron cargar los registros pendientes.');
        this.loading.set(false);
      },
    });
  }

  protected habilitar(usuario: UsuarioResponse): void {
    this.procesando.set(usuario.id_usuario);
    this.usuarioService.cambiarHabilitacion(usuario.id_usuario, true).subscribe({
      next: () => {
        this.pendientes.update((lista) => lista.filter((u) => u.id_usuario !== usuario.id_usuario));
        this.procesando.set(null);
        this.notifications.success(`${usuario.nombres} ${usuario.apellidos} ya puede iniciar sesión en el SGT.`);
      },
      error: () => this.procesando.set(null),
    });
  }

  protected pedirRechazo(usuario: UsuarioResponse): void {
    this.errorRechazo.set(null);
    this.aRechazar.set(usuario);
  }

  protected cancelarRechazo(): void {
    if (this.rechazando()) return;
    this.aRechazar.set(null);
  }

  protected confirmarRechazo(): void {
    const usuario = this.aRechazar();
    if (!usuario) return;
    this.rechazando.set(true);
    this.usuarioService.eliminar(usuario.id_usuario).subscribe({
      next: () => {
        this.pendientes.update((lista) => lista.filter((u) => u.id_usuario !== usuario.id_usuario));
        this.rechazando.set(false);
        this.aRechazar.set(null);
        this.notifications.success('Registro eliminado.');
      },
      error: (error: unknown) => {
        this.rechazando.set(false);
        this.errorRechazo.set(extractApiErrorMessage(error));
      },
    });
  }
}
