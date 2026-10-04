/** Contrato real de EspecialidadController (EspecialidadRequest/EspecialidadResponse en backend). */

export interface EspecialidadRequest {
  nombre: string;
  descripcion: string;
}

export interface EspecialidadResponse {
  id_especialidad: string;
  nombre: string;
  descripcion: string;
}

/** Especialidad del usuario autenticado (GET /especialidades/mias). */
export interface MiEspecialidadResponse {
  id_especialidad: string;
  nombre: string;
  es_responsable: boolean;
}

export interface MiembroEspecialidadResponse {
  id_usuario: string;
  nombres: string;
  apellidos: string;
  es_responsable: boolean;
}

export interface MiembroEspecialidadRequest {
  id_usuario: string;
  es_responsable: boolean;
}

/** PUT /especialidades/{id}/miembros: reemplaza el equipo completo. */
export interface EquipoEspecialidadRequest {
  miembros: MiembroEspecialidadRequest[];
}
