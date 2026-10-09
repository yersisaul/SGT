/**
 * Lecturas que todo rol tiene por defecto: el backend las suma al JWT al
 * iniciar sesión (Permisos.LECTURA_BASE, decisión 2026-10-09) y no se
 * asignan por rol. Mantener ambas listas iguales. No amplían el alcance: sin
 * *.read_all cada usuario solo ve lo propio.
 */
export const LECTURA_BASE: readonly string[] = [
  'solicitud.read',
  'requerimiento.read',
  'orden.read',
  'activo.read',
  'estado.read',
  'especialidad.read',
  'historial_solicitud.read',
  'historial_requerimiento.read',
  'historial_orden.read',
];

export function esLecturaBase(codigo: string): boolean {
  return LECTURA_BASE.includes(codigo);
}

/**
 * Como todos tienen el .read de Solicitudes, Requerimientos y Órdenes, el
 * menú de cada módulo se muestra a quien puede ACTUAR sobre él (o ver todo):
 * cualquiera de estos permisos habilita el módulo.
 */
export const PERMISOS_DE_MODULO = {
  solicitudes: [
    'solicitud.create',
    'solicitud.update',
    'solicitud.delete',
    'solicitud.generar_orden',
    'solicitud.generar_requerimiento',
    'solicitud.read_all',
    'historial_solicitud.create',
  ],
  requerimientos: [
    'requerimiento.create',
    'requerimiento.update',
    'requerimiento.delete',
    'requerimiento.aprobar',
    'requerimiento.generar_orden',
    'requerimiento.read_all',
    'historial_requerimiento.create',
  ],
  ordenes: [
    'orden.create',
    'orden.update',
    'orden.delete',
    'orden.tomar',
    'orden.asignar',
    'orden.verificar',
    'orden.reasignar',
    'orden.cerrar',
    'orden.read_all',
    'historial_orden.create',
  ],
} as const;

export type ModuloOperativo = keyof typeof PERMISOS_DE_MODULO;
