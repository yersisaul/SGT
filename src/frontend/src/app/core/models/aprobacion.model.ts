/** Contrato real de AprobacionRequest / AprobacionResponse
 * (backend: dto.request / dto.response). El mismo endpoint POST
 * /api/aprobaciones sirve para aprobar (aprobado: true) y rechazar
 * (aprobado: false); no existen dos endpoints separados. */

export interface AprobacionRequest {
  id_requerimiento: string;
  aprobado: boolean;
  comentario?: string | null;
  /** Solo al aprobar con el modal confirmado: genera la OT en esa especialidad (PRD D7/D17). */
  id_especialidad_orden?: string;
}

export interface AprobacionResponse {
  id_aprobacion: string;
  id_requerimiento: string;
  id_usuario: string;
  aprobado: boolean;
  comentario: string | null;
  fecha_aprobacion: string;
  url_adjunto: string | null;
}
