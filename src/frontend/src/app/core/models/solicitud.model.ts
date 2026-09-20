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
