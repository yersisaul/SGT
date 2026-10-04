package cfbd.co.sgt.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Calcula la fecha límite de despacho (fecha_registro + SLA) para Solicitud y
 * Requerimiento. Los valores en horas vienen de variables de entorno
 * (CLAUDE.md 7 / sección 9-12 del pedido de ajustes), nunca hardcodeados.
 */
@Component
public class SlaCalculator {

    private final long bajaHoras;
    private final long mediaHoras;
    private final long altaHoras;
    private final long requerimientoHoras;
    private final long atencionAltaHoras;
    private final long atencionMediaHoras;
    private final long atencionBajaHoras;

    public SlaCalculator(
            @Value("${app.sla.despacho.baja-horas:4}") long bajaHoras,
            @Value("${app.sla.despacho.media-horas:2}") long mediaHoras,
            @Value("${app.sla.despacho.alta-horas:1}") long altaHoras,
            @Value("${app.sla.despacho.requerimiento-horas:2}") long requerimientoHoras,
            @Value("${app.sla.atencion.alta-horas:8}") long atencionAltaHoras,
            @Value("${app.sla.atencion.media-horas:24}") long atencionMediaHoras,
            @Value("${app.sla.atencion.baja-horas:72}") long atencionBajaHoras) {
        this.atencionAltaHoras = atencionAltaHoras;
        this.atencionMediaHoras = atencionMediaHoras;
        this.atencionBajaHoras = atencionBajaHoras;
        this.bajaHoras = bajaHoras;
        this.mediaHoras = mediaHoras;
        this.altaHoras = altaHoras;
        this.requerimientoHoras = requerimientoHoras;
    }

    /** Deadline de despacho de una Solicitud según su prioridad (Baja/Media/Alta,
     * case-insensitive). Si la prioridad no es reconocida, se usa el SLA de "Media"
     * por defecto en lugar de fallar. */
    public Instant deadlineSolicitud(Instant fechaRegistro, String prioridad) {
        return fechaRegistro.plus(horasParaPrioridad(prioridad), ChronoUnit.HOURS);
    }

    /** Deadline de despacho de un Requerimiento: SLA único fijo, sin diferenciar
     * por prioridad (Requerimiento no tiene ese campo). */
    public Instant deadlineRequerimiento(Instant fechaRegistro) {
        return fechaRegistro.plus(requerimientoHoras, ChronoUnit.HOURS);
    }

    /** Deadline de atención (registro de la Solicitud → cierre de su OT), por prioridad (decisión 2026-10-04). */
    public Instant deadlineAtencion(Instant fechaRegistro, String prioridad) {
        long horas = prioridad == null ? atencionMediaHoras : switch (prioridad.trim().toLowerCase()) {
            case "alta" -> atencionAltaHoras;
            case "baja" -> atencionBajaHoras;
            default -> atencionMediaHoras;
        };
        return fechaRegistro.plus(horas, ChronoUnit.HOURS);
    }

    private long horasParaPrioridad(String prioridad) {
        if (prioridad == null) {
            return mediaHoras;
        }
        return switch (prioridad.trim().toLowerCase()) {
            case "baja" -> bajaHoras;
            case "alta" -> altaHoras;
            case "media" -> mediaHoras;
            default -> mediaHoras;
        };
    }
}
