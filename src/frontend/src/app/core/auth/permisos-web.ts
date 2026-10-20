/**
 * Permisos del panel del sitio web público (DataSeeder: web_catalogo,
 * web_comercial, web_novedad, web_documento). Tener cualquiera de estas
 * lecturas habilita el ingreso al panel; cada sección pide la suya.
 */
export const PERMISOS_LECTURA_WEB: readonly string[] = [
  'web_catalogo.read',
  'web_comercial.read',
  'web_novedad.read',
  'web_documento.read',
];

/** Prefijo de rutas del panel web: su login y su cierre de sesión son propios. */
export const RUTA_PANEL_WEB = '/web';
export const RUTA_LOGIN_WEB = '/web/login';
