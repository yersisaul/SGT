/** Contrato real de OrdenResponse (backend: dto.response.OrdenResponse).
 * id_usuario es el ejecutor de Operaciones responsable de la Orden (no quien
 * la generó). */
export interface OrdenResponse {
  id_orden: string;
  id_usuario: string;
  id_estado: string;
  id_especialidad: string;
  id_solicitud: string | null;
  id_requerimiento: string | null;
  numeroOrden: string;
  fecha_registro: string;
  fecha_cierre: string | null;
  url_adjunto: string | null;
}

/** Contrato real de OrdenRequest (backend: dto.request.OrdenRequest), body de
 * PUT /ordenes/{id}. OrdenServiceImpl.editarOrden ignora id_usuario,
 * id_especialidad, id_solicitud e id_requerimiento del body (preserva los
 * valores existentes) — solo id_estado (transición validada) se aplica
 * realmente; el adjunto se gestiona aparte vía ArchivoService/fileserver
 * propio, no por este PUT. Igual se envían los valores originales de esos
 * campos (no vacíos) por higiene del contrato. */
export interface OrdenRequest {
  id_usuario: string;
  id_estado: string;
  id_especialidad: string;
  id_solicitud: string | null;
  id_requerimiento: string | null;
}

/** Contrato real de GenerarOrdenRequest (backend: dto.request.GenerarOrdenRequest),
 * body de POST /solicitudes/{id}/generar-orden y /requerimientos/{id}/generar-orden.
 * id_usuario_ejecutor es obligatorio (@NotNull en backend): no se puede
 * generar una OT sin ejecutor de Operaciones asignado. */
export interface GenerarOrdenRequest {
  id_usuario_ejecutor: string;
  comentario?: string;
  url_adjunto?: string | null;
}

/** Contrato real de CerrarOrdenRequest (backend: dto.request.CerrarOrdenRequest),
 * body de POST /ordenes/{id}/cerrar. */
export interface CerrarOrdenRequest {
  comentario?: string;
}

/** Contrato real de ReasignarOrdenRequest (backend: dto.request.ReasignarOrdenRequest),
 * body de POST /ordenes/{id}/reasignar. */
export interface ReasignarOrdenRequest {
  id_usuario_nuevo: string;
  comentario?: string;
}

/** Contrato real de HistorialOrdenResponse (backend: dto.response). */
export interface HistorialOrdenResponse {
  id_historial_orden: string;
  id_orden: string;
  id_usuario: string;
  id_estado_anterior: string;
  id_estado_nuevo: string;
  fecha: string;
  comentario: string | null;
}
