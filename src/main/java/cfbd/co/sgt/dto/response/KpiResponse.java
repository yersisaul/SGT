package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Resultado de una familia de KPI (PRD E6). Forma genérica para que el
 * dashboard y el CSV reutilicen la misma estructura en las 4 familias.
 *
 * @param alcance    "Global" o los nombres de las especialidades a las que se recortó.
 * @param instantaneo true si el KPI es una foto del momento (no depende del rango).
 * @param filas      cada fila es clave de columna → valor (texto o número).
 */
public record KpiResponse(String familia, String titulo, Instant desde, Instant hasta, List<String> alcance,
                          boolean instantaneo, List<KpiIndicadorResponse> indicadores,
                          List<KpiColumnaResponse> columnas, List<Map<String, Object>> filas) {
}
