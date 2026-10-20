import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';

import { AuthService } from '../../../../core/auth/auth.service';
import { ClaveSla, ConfiguracionSlaResponse } from '../../../../core/models/sla.model';
import { ConfiguracionSlaService } from '../../../../core/services/configuracion-sla.service';
import { Button } from '../../../../shared/components/button/button';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import { LucideCircleAlert, LucideClock } from '../../../../shared/icons/lucide-icons';
import { NotificationService } from '../../../../shared/services/notification.service';

const MIN_HORAS = 1;
const MAX_HORAS = 720;

interface GrupoSla {
  id: 'despacho' | 'atencion';
  titulo: string;
  descripcion: string;
  items: ConfiguracionSlaResponse[];
}

/**
 * Tiempos de SLA editables por el Administrador (observación 2026-10-19).
 * El cambio aplica de inmediato a los cálculos de límite, a los KPI y al
 * aviso por correo de Requerimientos vencidos.
 */
@Component({
  selector: 'app-sla',
  imports: [DatePipe, Button, Spinner, LucideCircleAlert, LucideClock],
  templateUrl: './sla.html',
  styleUrl: './sla.css',
})
export class Sla {
  private readonly authService = inject(AuthService);
  private readonly slaService = inject(ConfiguracionSlaService);
  private readonly notifications = inject(NotificationService);

  protected readonly canUpdate = this.authService.hasPermission('sla.update');
  protected readonly minHoras = MIN_HORAS;
  protected readonly maxHoras = MAX_HORAS;

  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly saving = signal(false);

  private readonly guardados = signal<ConfiguracionSlaResponse[]>([]);
  /** Valores editados (texto del input) por clave. */
  protected readonly borrador = signal<Partial<Record<ClaveSla, string>>>({});

  protected readonly grupos = computed<GrupoSla[]>(() => [
    {
      id: 'despacho',
      titulo: 'Despacho',
      descripcion:
        'Tiempo máximo desde el registro hasta que el Despachador clasifica la Solicitud, o el Administrador decide el Requerimiento.',
      items: this.guardados().filter((sla) => sla.grupo === 'despacho'),
    },
    {
      id: 'atencion',
      titulo: 'Atención',
      descripcion: 'Tiempo máximo desde el registro de la Solicitud hasta el cierre de su orden de trabajo (bajo contrato).',
      items: this.guardados().filter((sla) => sla.grupo === 'atencion'),
    },
  ]);

  protected readonly cambios = computed(() =>
    this.guardados()
      .filter((sla) => {
        const editado = this.borrador()[sla.clave];
        return editado !== undefined && Number(editado) !== sla.horas;
      })
      .map((sla) => ({ clave: sla.clave, horas: Number(this.borrador()[sla.clave]) })),
  );

  protected readonly hayErrores = computed(() =>
    this.guardados().some((sla) => this.error(sla.clave) !== null),
  );

  constructor() {
    this.cargar();
  }

  protected reload(): void {
    this.cargar();
  }

  protected valor(sla: ConfiguracionSlaResponse): string {
    return this.borrador()[sla.clave] ?? String(sla.horas);
  }

  protected editar(clave: ClaveSla, texto: string): void {
    this.borrador.update((actual) => ({ ...actual, [clave]: texto }));
  }

  protected restablecer(sla: ConfiguracionSlaResponse): void {
    this.editar(sla.clave, String(sla.horas_por_defecto));
  }

  protected error(clave: ClaveSla): string | null {
    const texto = this.borrador()[clave];
    if (texto === undefined) return null;
    const horas = Number(texto);
    if (texto.trim() === '' || !Number.isInteger(horas)) return 'Ingresa un número entero de horas.';
    if (horas < MIN_HORAS || horas > MAX_HORAS) return `Entre ${MIN_HORAS} y ${MAX_HORAS} horas.`;
    return null;
  }

  protected modificado(sla: ConfiguracionSlaResponse): boolean {
    const editado = this.borrador()[sla.clave];
    return editado !== undefined && Number(editado) !== sla.horas;
  }

  /** "2 h" o "3 d 4 h" para leer de un vistazo valores largos. */
  protected equivalencia(sla: ConfiguracionSlaResponse): string | null {
    const horas = Number(this.valor(sla));
    if (!Number.isInteger(horas) || horas < 24) return null;
    const dias = Math.floor(horas / 24);
    const resto = horas % 24;
    return resto === 0 ? `${dias} d` : `${dias} d ${resto} h`;
  }

  protected descartar(): void {
    this.borrador.set({});
  }

  protected guardar(): void {
    const valores = this.cambios();
    if (valores.length === 0 || this.hayErrores() || this.saving()) return;
    this.saving.set(true);
    this.slaService.actualizar({ valores }).subscribe({
      next: (actualizados) => {
        this.guardados.set(actualizados);
        this.borrador.set({});
        this.saving.set(false);
        this.notifications.success(
          valores.length === 1 ? 'SLA actualizado.' : `${valores.length} SLA actualizados.`,
        );
      },
      error: () => this.saving.set(false),
    });
  }

  private cargar(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.slaService.listar().subscribe({
      next: (slas) => {
        this.guardados.set(slas);
        this.borrador.set({});
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudo cargar la configuración de SLA.');
        this.loading.set(false);
      },
    });
  }
}
