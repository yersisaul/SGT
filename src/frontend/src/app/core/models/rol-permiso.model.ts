/** Contrato real de RolPermisoController: solo asignar (create) y revocar
 * (delete) — no existe "editar" una asociación rol-permiso (ver comentario
 * del propio controller en backend). */

export interface RolPermisoRequest {
  id_rol: string;
  id_permiso: string;
}

export interface RolPermisoResponse {
  id_rol_permiso: string;
  id_rol: string;
  nombre_rol: string;
  id_permiso: string;
  codigo_permiso: string;
}
