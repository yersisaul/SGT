import { RequerimientoResponse } from '../../../core/models/requerimiento.model';

/**
 * Forma de presentación de un Requerimiento: el DTO real (solo ids)
 * enriquecido con los nombres resueltos desde los catálogos (Estado/
 * Especialidad/Usuario) — mismo criterio que solicitud-view.model.ts.
 * Sin activoNombre: Requerimiento no tiene id_activo.
 */
export interface RequerimientoView {
  raw: RequerimientoResponse;
  estadoNombre: string;
  especialidadNombre: string;
  /** Quien CREÓ el requerimiento (Despachador), no un "solicitante" cliente
   * — Requerimiento no representa al cliente final, a diferencia de Solicitud. */
  creadoPorNombre: string;
}

export interface RequerimientoKanbanColumn {
  estadoId: string;
  estadoNombre: string;
  items: RequerimientoView[];
  transicionable: boolean;
}
