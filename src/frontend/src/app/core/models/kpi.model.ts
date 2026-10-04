/** Contrato real de GET /api/kpis/{familia} (backend: dto.response.KpiResponse). */
export type FamiliaKpi = 'sla-despacho' | 'sla-atencion' | 'decision-rq' | 'tiempos-ciclo' | 'cola-carga' | 'ruteo';

/** Resultado contra la meta aprobada (2026-10-04). */
export type EstadoMeta = 'cumple' | 'alerta' | 'no_cumple';

export interface KpiIndicador {
  clave: string;
  etiqueta: string;
  /** null = sin datos suficientes en el rango (no se muestra un 0 inventado). */
  valor: number | null;
  unidad: '%' | 'h' | '';
  /** Meta del indicador; null si no tiene meta. */
  meta: number | null;
  /** null = sin meta o sin datos. */
  estado: EstadoMeta | null;
}

export interface KpiColumna {
  clave: string;
  etiqueta: string;
  unidad: '%' | 'h' | '';
}

export interface KpiResponse {
  familia: FamiliaKpi;
  titulo: string;
  desde: string;
  hasta: string;
  /** "Global" o las especialidades a las que se recortó (FR-037). */
  alcance: string[];
  /** true: foto del momento, no depende del rango (cola-carga). */
  instantaneo: boolean;
  indicadores: KpiIndicador[];
  columnas: KpiColumna[];
  filas: Record<string, string | number | null>[];
}

export interface RangoKpi {
  /** yyyy-MM-dd, inclusive. */
  desde: string;
  hasta: string;
}
