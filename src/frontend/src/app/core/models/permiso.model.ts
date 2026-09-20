/** Contrato real de PermisoController (PermisoRequest/PermisoResponse en backend).
 * "codigo" sigue la nomenclatura <recurso>.<accion> (p.ej. "solicitud.read"). */

export interface PermisoRequest {
  codigo: string;
  descripcion: string;
}

export interface PermisoResponse {
  id_permiso: string;
  codigo: string;
  descripcion: string;
}
