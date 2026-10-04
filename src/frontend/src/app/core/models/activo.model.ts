/** Contrato real de ActivoController (ActivoRequest/ActivoResponse en backend). */

export interface ActivoRequest {
  /** Especialidad principal: con ella nace la Solicitud. */
  id_especialidad: string;
  /** Todas las especialidades del activo (la principal se agrega igual en el backend). */
  ids_especialidad: string[];
  codigo: string;
  nombre: string;
  descripcion: string | null;
  ubicacion: string | null;
}

export interface ActivoResponse {
  id_activo: string;
  id_especialidad: string;
  ids_especialidad: string[];
  codigo: string;
  nombre: string;
  descripcion: string | null;
  ubicacion: string | null;
  url_img: string | null;
}
