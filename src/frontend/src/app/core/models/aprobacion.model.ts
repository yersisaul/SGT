/** Contrato real de AprobacionRequest / AprobacionResponse
 * (backend: dto.request / dto.response). El mismo endpoint POST
 * /api/aprobaciones sirve para aprobar (aprobado: true) y rechazar
 * (aprobado: false); no existen dos endpoints separados. */

export interface AprobacionRequest {
  id_requerimiento: string;
  aprobado: boolean;
  comentario?: string | null;
  url_adjunto?: string | null;
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
