package cfbd.co.sgt.model;

/**
 * Parámetros de SLA editables desde el panel (aprobado 2026-10-19). Los
 * valores iniciales vienen de las variables de entorno SLA_* (ver
 * ConfiguracionSlaServiceImpl).
 */
public enum ClaveSla {
    DESPACHO_ALTA("despacho", "Despacho de Solicitud · prioridad Alta"),
    DESPACHO_MEDIA("despacho", "Despacho de Solicitud · prioridad Media"),
    DESPACHO_BAJA("despacho", "Despacho de Solicitud · prioridad Baja"),
    DESPACHO_REQUERIMIENTO("despacho", "Decisión de Requerimiento"),
    ATENCION_ALTA("atencion", "Atención · prioridad Alta"),
    ATENCION_MEDIA("atencion", "Atención · prioridad Media"),
    ATENCION_BAJA("atencion", "Atención · prioridad Baja");

    private final String grupo;
    private final String etiqueta;

    ClaveSla(String grupo, String etiqueta) {
        this.grupo = grupo;
        this.etiqueta = etiqueta;
    }

    public String grupo() {
        return grupo;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
