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
