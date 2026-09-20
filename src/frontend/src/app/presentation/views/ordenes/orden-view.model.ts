import { OrdenResponse } from '../../../core/models/orden.model';
import { RequerimientoResponse } from '../../../core/models/requerimiento.model';
import { SolicitudResponse } from '../../../core/models/solicitud.model';

/**
 * Forma de presentación de una Orden: el DTO real enriquecido con los
 * nombres resueltos desde los catálogos — mismo criterio que
 * solicitud-view.model.ts / requerimiento-view.model.ts. Sin datos de
 * origen: esos se cargan bajo demanda al abrir el detalle (ver
 * OrigenContexto), no en cada tarjeta de la bandeja.
 *
 * Sin "generadaPorNombre": OrdenResponse NO expone id_usuario (a diferencia
 * de SolicitudResponse/RequerimientoResponse), aunque el modelo Orden y
 * OrdenRequest sí lo tienen — es un campo que el backend no devuelve, así
 * que no se puede resolver "quién generó la Orden" sin inventar el dato.
 */
export interface OrdenView {
  raw: OrdenResponse;
  estadoNombre: string;
  especialidadNombre: string;
}

export interface OrdenKanbanColumn {
  estadoId: string;
  estadoNombre: string;
  items: OrdenView[];
  transicionable: boolean;
}

/**
 * Contexto de origen resuelto para el detalle (instrucción 5/6): distingue
 * Solicitud vs Requerimiento según cuál de id_solicitud/id_requerimiento
 * viene con valor — nunca ambos, nunca ninguno inventado.
 */
export type OrigenContexto =
  | { tipo: 'solicitud'; data: SolicitudResponse }
  | { tipo: 'requerimiento'; data: RequerimientoResponse }
  | { tipo: 'sin-acceso' }
  | null;
