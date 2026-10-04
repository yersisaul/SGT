/** Contrato real de OrdenResponse (backend: dto.response.OrdenResponse).
 * id_usuario es el ejecutor de Operaciones responsable de la Orden (no quien
 * la generó). */
export interface OrdenResponse {
  id_orden: string;
  /** Ejecutor; null mientras la OT está en la cola de su especialidad o "Devuelta". */
  id_usuario: string | null;
  nombre_ejecutor: string | null;
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
/** La OT se genera en la cola de la especialidad elegida (PRD D3/D16), sin ejecutor. */
export interface GenerarOrdenRequest {
  id_especialidad: string;
  comentario?: string;
}

/** Contrato real de CerrarOrdenRequest (backend: dto.request.CerrarOrdenRequest),
 * body de POST /ordenes/{id}/cerrar. */
export interface CerrarOrdenRequest {
  comentario?: string;
}

/** Contrato real de ReasignarOrdenRequest (backend: dto.request.ReasignarOrdenRequest),
 * body de POST /ordenes/{id}/reasignar. */
/** Reasignar a otra especialidad (paso 12): responsable de la especialidad o Administrador. */
export interface ReasignarOrdenRequest {
  id_especialidad_destino: string;
  motivo: string;
}

export interface AsignarOrdenRequest {
  id_usuario: string;
}

/** Paso 11: si corresponde=false, el motivo es obligatorio y la OT queda "Devuelta". */
export interface VerificarOrdenRequest {
  corresponde: boolean;
  motivo?: string;
}

export interface CargaMiembroResponse {
  id_usuario: string;
  nombres: string;
  apellidos: string;
  es_responsable: boolean;
  ordenes_abiertas: number;
}

export type TipoAsignacionOrden =
  | 'ENCOLADA'
  | 'TOMADA'
  | 'ASIGNADA'
  | 'CONFIRMADA'
  | 'DEVUELTA'
  | 'REASIGNADA_ESPECIALIDAD';

export interface AsignacionOrdenResponse {
  id_asignacion_orden: string;
  id_orden: string;
  tipo: TipoAsignacionOrden;
  id_especialidad_origen: string | null;
  id_especialidad_destino: string;
  id_usuario_origen: string | null;
  id_usuario_destino: string | null;
  id_usuario_actor: string;
  nombre_actor: string;
  motivo: string | null;
  fecha: string;
}

/** Contrato real de HistorialOrdenResponse (backend: dto.response). */
export interface HistorialOrdenResponse {
  id_historial_orden: string;
  id_orden: string;
  id_usuario: string;
  nombre_usuario: string;
  id_estado_anterior: string;
  id_estado_nuevo: string;
  fecha: string;
  comentario: string | null;
}

/** OT que superó el umbral de espera en cola (GET /ordenes/escaladas). */
export interface OrdenEscaladaResponse {
  id_orden: string;
  numero_orden: string;
  id_especialidad: string;
  especialidad: string;
  prioridad: string;
  en_cola_desde: string;
  minutos_en_cola: number;
  /** 1 = avisada a los responsables; 2 = también al Administrador. */
  nivel: 1 | 2;
}
