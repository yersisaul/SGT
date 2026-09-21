/** Contrato real de SolicitudResponse / SolicitudRequest (backend: dto.response / dto.request). */

export interface SolicitudResponse {
  id_solicitud: string;
  id_usuario: string;
  id_activo: string;
  id_estado: string;
  id_especialidad: string;
  numeroSolicitud: string;
  prioridad: string;
  fecha_registro: string;
  descripcion: string;
  url_adjunto: string | null;
  /** Derivado en backend (fecha_registro + SLA según prioridad), no
   * persistido. El front lo muestra solo mientras la Solicitud sigue
   * "Pendiente" (ver shared/utils/sla.util.ts). */
  fecha_limite_despacho: string | null;
}

/** Contrato real de GenerarRequerimientoRequest (backend: dto.request),
 * body de POST /solicitudes/{id}/generar-requerimiento. Si "descripcion" se
 * omite/vacía, el backend la autogenera (descripción original de la
 * Solicitud + nota de origen) — nunca queda en blanco. */
export interface GenerarRequerimientoRequest {
  descripcion?: string;
}

export interface SolicitudRequest {
  id_usuario: string;
  id_activo: string;
  id_estado: string;
  id_especialidad: string;
  prioridad: string;
  descripcion: string;
  url_adjunto: string | null;
}

/** Contrato real de POST /api/historial-solicitudes (HistorialSolicitudRequest). */
export interface HistorialSolicitudRequest {
  id_solicitud: string;
  id_estado_anterior: string;
  id_estado_nuevo: string;
  comentario?: string;
}

/** Contrato real de HistorialSolicitudResponse (backend: dto.response). */
export interface HistorialSolicitudResponse {
  id_historial_solicitud: string;
  id_solicitud: string;
  id_usuario: string;
  id_estado_anterior: string;
  id_estado_nuevo: string;
  fecha: string;
  comentario: string | null;
}
