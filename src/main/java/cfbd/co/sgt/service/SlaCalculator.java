package cfbd.co.sgt.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.model.ClaveSla;

/**
 * Calcula la fecha límite de despacho (fecha_registro + SLA) para Solicitud y
 * Requerimiento, y la de atención. Las horas se leen de configuracion_sla
 * (editables por el Administrador; valor inicial desde variables de entorno).
 */
@Component
public class SlaCalculator {

    private final ConfiguracionSlaService configuracion;

    public SlaCalculator(ConfiguracionSlaService configuracion) {
        this.configuracion = configuracion;
    }

    /** Deadline de despacho de una Solicitud según su prioridad (Baja/Media/Alta,
     * case-insensitive). Si la prioridad no es reconocida, se usa el SLA de "Media"
     * por defecto en lugar de fallar. */
    public Instant deadlineSolicitud(Instant fechaRegistro, String prioridad) {
        ClaveSla clave = switch (normalizar(prioridad)) {
            case "baja" -> ClaveSla.DESPACHO_BAJA;
            case "alta" -> ClaveSla.DESPACHO_ALTA;
            default -> ClaveSla.DESPACHO_MEDIA;
        };
        return sumar(fechaRegistro, clave);
    }

    /** Deadline de despacho de un Requerimiento: SLA único fijo, sin diferenciar
     * por prioridad (Requerimiento no tiene ese campo). */
    public Instant deadlineRequerimiento(Instant fechaRegistro) {
        return sumar(fechaRegistro, ClaveSla.DESPACHO_REQUERIMIENTO);
    }

    /** Deadline de atención (registro de la Solicitud → cierre de su OT), por prioridad (decisión 2026-10-04). */
    public Instant deadlineAtencion(Instant fechaRegistro, String prioridad) {
        ClaveSla clave = switch (normalizar(prioridad)) {
            case "alta" -> ClaveSla.ATENCION_ALTA;
            case "baja" -> ClaveSla.ATENCION_BAJA;
            default -> ClaveSla.ATENCION_MEDIA;
        };
        return sumar(fechaRegistro, clave);
    }

    private Instant sumar(Instant fechaRegistro, ClaveSla clave) {
        return fechaRegistro.plus(configuracion.horas(clave), ChronoUnit.HOURS);
    }

    private static String normalizar(String prioridad) {
        return prioridad == null ? "" : prioridad.trim().toLowerCase();
    }
}
