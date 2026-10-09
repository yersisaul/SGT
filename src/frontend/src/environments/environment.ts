/**
 * Producción (build por defecto): nginx sirve el frontend y hace de proxy de
 * /api hacia el backend en el mismo origen, por lo que no hace falta CORS.
 */
export const environment = {
  apiBaseUrl: '/api',
};
