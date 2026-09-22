/** Contrato real de UsuarioController (UsuarioRequest/UsuarioResponse en backend). */

export interface UsuarioRequest {
  id_rol: string;
  email: string;
  /** En edición: vacío/omitido = no cambiar contraseña (UsuarioServiceImpl solo la
   * actualiza si viene no-blank). En creación es obligatoria. */
  password: string;
  nombres: string;
  apellidos: string;
}

export interface UsuarioResponse {
  id_usuario: string;
  email: string;
  nombres: string;
  apellidos: string;
  url_img: string | null;
  id_rol: string;
  nombre_rol: string;
}
