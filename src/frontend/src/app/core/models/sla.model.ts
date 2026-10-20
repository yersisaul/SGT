/** Contrato de ConfiguracionSlaController (GET/PUT /api/configuracion/sla). */

export type ClaveSla =
  | 'DESPACHO_ALTA'
  | 'DESPACHO_MEDIA'
  | 'DESPACHO_BAJA'
  | 'DESPACHO_REQUERIMIENTO'
  | 'ATENCION_ALTA'
  | 'ATENCION_MEDIA'
  | 'ATENCION_BAJA';

export interface ConfiguracionSlaResponse {
  clave: ClaveSla;
  grupo: 'despacho' | 'atencion';
  etiqueta: string;
  horas: number;
  /** Valor inicial del .env (para "restablecer"). */
  horas_por_defecto: number;
  fecha_modificacion: string | null;
  modificado_por: string | null;
}

export interface ConfiguracionSlaRequest {
  valores: { clave: ClaveSla; horas: number }[];
}
