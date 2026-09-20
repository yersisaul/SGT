/** Contrato real de GET /api/solicitudes/resumen-estados y
 * GET /api/requerimientos/resumen-estados (ResumenEstadosResponse en el backend). */

export interface EstadoCantidad {
  estado: string;
  cantidad: number;
}

export interface ResumenEstados {
  total: number;
  porEstado: EstadoCantidad[];
}
