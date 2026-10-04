package cfbd.co.sgt.service.kpi;

/**
 * Meta de un indicador.
 *
 * @param alerta      umbral intermedio (null = sin banda de alerta).
 * @param mayorEsMejor true para porcentajes de cumplimiento; false para tiempos y tasas de error.
 */
record MetaKpi(double valor, Double alerta, boolean mayorEsMejor) {

    static final String CUMPLE = "cumple";
    static final String ALERTA = "alerta";
    static final String NO_CUMPLE = "no_cumple";

    String evaluar(double observado) {
        if (cumple(observado, valor)) {
            return CUMPLE;
        }
        return alerta != null && cumple(observado, alerta) ? ALERTA : NO_CUMPLE;
    }

    private boolean cumple(double observado, double umbral) {
        return mayorEsMejor ? observado >= umbral : observado <= umbral;
    }
}
