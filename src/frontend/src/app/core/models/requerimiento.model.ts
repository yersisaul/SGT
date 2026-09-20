/** Contrato real de RequerimientoResponse / RequerimientoRequest
 * (backend: dto.response / dto.request). Sin id_activo ni id_solicitud: el
 * modelo actual de Requerimiento no tiene esos campos — no inventarlos. */

export interface RequerimientoResponse {
  id_requerimiento: string;
  id_usuario: string;
  id_estado: string;
  id_especialidad: string;
  numeroRequerimiento: string;
  fecha_registro: string;
  descripcion: string;
  url_adjunto: string | null;
}

export interface RequerimientoRequest {
  id_usuario: string;
  id_estado: string;
  id_especialidad: string;
  descripcion: string;
  url_adjunto: string | null;
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
