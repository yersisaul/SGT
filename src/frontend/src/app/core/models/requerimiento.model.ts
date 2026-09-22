/** Contrato real de RequerimientoResponse / RequerimientoRequest
 * (backend: dto.response / dto.request). Sin id_activo: el modelo actual de
 * Requerimiento no tiene ese campo — no inventarlo. */

export interface RequerimientoResponse {
  id_requerimiento: string;
  id_usuario: string;
  id_estado: string;
  id_especialidad: string;
  /** Solicitud de origen (Requerimiento.solicitud), si se generó desde una
   * Solicitud fuera de contrato vía POST /solicitudes/{id}/generar-requerimiento;
   * null si es independiente. */
  id_solicitud: string | null;
  numeroRequerimiento: string;
  fecha_registro: string;
  descripcion: string;
  url_adjunto: string | null;
  /** Derivado en backend (fecha_registro + SLA fijo de Requerimiento), no
   * persistido. El front lo muestra solo mientras el Requerimiento sigue
   * "Pendiente" (ver shared/utils/sla.util.ts). */
  fecha_limite_despacho: string | null;
}

export interface RequerimientoRequest {
  id_usuario: string;
  id_estado: string;
  id_especialidad: string;
  descripcion: string;
}

/** Contrato real de HistorialRequerimientoResponse (backend: dto.response). */
export interface HistorialRequerimientoResponse {
  id_historial_requerimiento: string;
  id_requerimiento: string;
  id_usuario: string;
  id_estado_anterior: string;
  id_estado_nuevo: string;
  fecha: string;
  comentario: string | null;
}
