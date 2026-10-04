/** Catálogos consumidos por formularios/tableros que referencian Solicitud.
 * Contratos reales: EstadoResponse, ActivoResponse, EspecialidadResponse, UsuarioResponse. */

export interface EstadoCatalogo {
  id_estado: string;
  nombre: string;
}

export interface ActivoCatalogo {
  id_activo: string;
  /** Especialidad principal. */
  id_especialidad: string;
  /** Todas las especialidades en las que el activo puede tener Solicitudes. */
  ids_especialidad: string[];
  codigo: string;
  nombre: string;
  descripcion: string | null;
  ubicacion: string | null;
  url_img: string | null;
}

export interface EspecialidadCatalogo {
  id_especialidad: string;
  nombre: string;
  descripcion: string | null;
}

export interface UsuarioCatalogo {
  id_usuario: string;
  email: string;
  nombres: string;
  apellidos: string;
  url_img: string | null;
  id_rol: string;
  nombre_rol: string;
}

export interface RolCatalogo {
  id_rol: string;
  nombre: string;
  descripcion: string;
}
