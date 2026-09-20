/** Contrato real de RolController (RolRequest/RolResponse en backend). */

export interface RolRequest {
  nombre: string;
  descripcion: string;
}

export interface RolResponse {
  id_rol: string;
  nombre: string;
  descripcion: string;
}
