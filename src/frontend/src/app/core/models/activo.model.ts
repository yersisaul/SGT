/** Contrato real de ActivoController (ActivoRequest/ActivoResponse en backend). */

export interface ActivoRequest {
  id_especialidad: string;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  ubicacion: string | null;
}

export interface ActivoResponse {
  id_activo: string;
  id_especialidad: string;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  ubicacion: string | null;
  url_img: string | null;
}
