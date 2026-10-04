package cfbd.co.sgt.dto.response;

/** Columna del detalle tabular de un KPI (también es la cabecera del CSV). */
public record KpiColumnaResponse(String clave, String etiqueta, String unidad) {
}
