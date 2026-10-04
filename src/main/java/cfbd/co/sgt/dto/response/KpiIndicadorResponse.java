package cfbd.co.sgt.dto.response;

/**
 * Indicador resumen de una familia de KPI (tarjeta del dashboard).
 *
 * @param valor  null cuando no hay datos suficientes en el rango (no se inventa un 0).
 * @param unidad "%", "h" o "" (conteo).
 * @param meta   meta aprobada (2026-10-04) o null si el indicador no tiene meta.
 * @param estado "cumple", "alerta", "no_cumple" o null (sin meta o sin datos).
 */
public record KpiIndicadorResponse(String clave, String etiqueta, Double valor, String unidad, Double meta,
                                   String estado) {

    /** Indicador sin meta; MetasKpi la agrega después si corresponde. */
    public static KpiIndicadorResponse de(String clave, String etiqueta, Double valor, String unidad) {
        return new KpiIndicadorResponse(clave, etiqueta, valor, unidad, null, null);
    }
}
