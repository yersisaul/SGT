import { SolicitudResponse } from '../../../core/models/solicitud.model';

/**
 * Forma de presentación de una Solicitud: el DTO real (SolicitudResponse,
 * solo ids) enriquecido con los nombres resueltos desde los catálogos
 * (Estado/Activo/Especialidad/Usuario) para mostrar en Kanban y tabla sin
 * repetir la resolución en cada componente.
 */
export interface SolicitudView {
  raw: SolicitudResponse;
  estadoNombre: string;
  activoNombre: string;
  especialidadNombre: string;
  usuarioNombre: string;
}

export interface KanbanColumn {
  estadoId: string;
  estadoNombre: string;
  items: SolicitudView[];
  /** false para columnas terminales/de decisión (p.ej. "Finalizado"): no
   * aceptan drop ni permiten arrastrar sus tarjetas — se alcanzan solo por
   * una acción de negocio explícita (ver solicitud-estados.config.ts). */
  transicionable: boolean;
}
