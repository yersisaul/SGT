import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { LucideDownload } from '@lucide/angular';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { AuthService } from '../../../../../core/auth/auth.service';
import { EstadoMeta, FamiliaKpi, KpiColumna, KpiIndicador, KpiResponse, RangoKpi } from '../../../../../core/models/kpi.model';
import { Badge, BadgeVariant } from '../../../../../shared/components/badge/badge';
import { KpiService } from '../../../../../core/services/kpi.service';
import { Button } from '../../../../../shared/components/button/button';
import { Card } from '../../../../../shared/components/card/card';
import { Spinner } from '../../../../../shared/components/spinner/spinner';
import { NotificationService } from '../../../../../shared/services/notification.service';

type Preset = '7' | '30' | '90' | 'personalizado';

const FAMILIAS: FamiliaKpi[] = ['sla-despacho', 'sla-atencion', 'decision-rq', 'tiempos-ciclo', 'cola-carga', 'ruteo'];

/** Indicadores donde un valor menor es mejor (tiempos, tasas de error, carga). */
const MENOR_ES_MEJOR = new Set(['cola_mediana', 'cola_p90', 'tasa_reasignacion', 'tasa_devolucion', 'carga_max']);

const ETIQUETA_ESTADO: Record<EstadoMeta, string> = {
  cumple: 'Cumple',
  alerta: 'En alerta',
  no_cumple: 'No cumple',
};

const VARIANTE_ESTADO: Record<EstadoMeta, BadgeVariant> = {
  cumple: 'success',
  alerta: 'warning',
  no_cumple: 'danger',
};
const DIAS_POR_PRESET: Record<Exclude<Preset, 'personalizado'>, number> = { '7': 7, '30': 30, '90': 90 };
const HTTP_FORBIDDEN = 403;

function fechaIso(fecha: Date): string {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${anio}-${mes}-${dia}`;
}

function rangoDesdeHoy(dias: number): RangoKpi {
  const hasta = new Date();
  const desde = new Date();
  desde.setDate(hasta.getDate() - dias);
  return { desde: fechaIso(desde), hasta: fechaIso(hasta) };
}

/**
 * KPIs por rol (PRD FR-030…FR-037): fila de indicadores + tabla de detalle por
 * familia, con rango de fechas y exportación CSV. El backend recorta el
 * alcance (global o especialidades del responsable); si el usuario no tiene
 * alcance (403), el panel no se muestra.
 */
@Component({
  selector: 'app-kpi-panel',
  imports: [DecimalPipe, FormsModule, Badge, Button, Card, Spinner, LucideDownload],
  templateUrl: './kpi-panel.html',
  styleUrl: './kpi-panel.css',
})
export class KpiPanel {
  private readonly kpiService = inject(KpiService);
  private readonly authService = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected readonly canExport = this.authService.hasPermission('kpi.export');

  protected readonly preset = signal<Preset>('30');
  protected readonly desde = signal(rangoDesdeHoy(DIAS_POR_PRESET['30']).desde);
  protected readonly hasta = signal(rangoDesdeHoy(DIAS_POR_PRESET['30']).hasta);

  protected readonly loading = signal(true);
  protected readonly sinAlcance = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly kpis = signal<KpiResponse[]>([]);
  protected readonly exportando = signal<FamiliaKpi | null>(null);

  protected readonly rangoValido = computed(() => this.desde() <= this.hasta());

  constructor() {
    this.cargar();
  }

  protected elegirPreset(preset: Preset): void {
    this.preset.set(preset);
    if (preset !== 'personalizado') {
      const rango = rangoDesdeHoy(DIAS_POR_PRESET[preset]);
      this.desde.set(rango.desde);
      this.hasta.set(rango.hasta);
      this.cargar();
    }
  }

  protected aplicarPersonalizado(): void {
    if (this.rangoValido()) {
      this.cargar();
    }
  }

  protected formato(valor: number | string | null | undefined, unidad: KpiColumna['unidad']): string {
    if (valor === null || valor === undefined || valor === '') return '—';
    if (typeof valor === 'string') return valor;
    return unidad === '' ? String(valor) : `${valor} ${unidad}`;
  }

  protected textoMeta(indicador: KpiIndicador): string {
    const comparador = MENOR_ES_MEJOR.has(indicador.clave) ? '≤' : '≥';
    return `Meta ${comparador} ${indicador.meta}${indicador.unidad === '' ? '' : ' ' + indicador.unidad}`;
  }

  protected etiquetaEstado(estado: EstadoMeta): string {
    return ETIQUETA_ESTADO[estado];
  }

  protected varianteEstado(estado: EstadoMeta): BadgeVariant {
    return VARIANTE_ESTADO[estado];
  }

  protected exportar(familia: FamiliaKpi): void {
    this.exportando.set(familia);
    this.kpiService.exportarCsv(familia, this.rango()).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `kpi-${familia}-${this.desde()}-${this.hasta()}.csv`;
        enlace.click();
        URL.revokeObjectURL(url);
        this.exportando.set(null);
      },
      error: () => {
        this.notifications.error('No se pudo exportar el CSV.');
        this.exportando.set(null);
      },
    });
  }

  private rango(): RangoKpi {
    return { desde: this.desde(), hasta: this.hasta() };
  }

  private cargar(): void {
    this.loading.set(true);
    this.error.set(null);
    const rango = this.rango();
    forkJoin(
      FAMILIAS.map((familia) =>
        this.kpiService.obtener(familia, rango).pipe(
          map((kpi): KpiResponse | 'sin-alcance' | null => kpi),
          catchError((error: unknown) =>
            of(error instanceof HttpErrorResponse && error.status === HTTP_FORBIDDEN ? 'sin-alcance' : null),
          ),
        ),
      ),
    ).subscribe((resultados) => {
      this.sinAlcance.set(resultados.every((r) => r === 'sin-alcance'));
      const validos = resultados.filter((r): r is KpiResponse => r !== null && r !== 'sin-alcance');
      this.kpis.set(validos);
      if (!this.sinAlcance() && validos.length === 0) {
        this.error.set('No se pudieron cargar los indicadores.');
      }
      this.loading.set(false);
    });
  }
}
