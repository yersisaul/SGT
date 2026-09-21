package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequerimientoResponse {
    private UUID id_requerimiento;
    private UUID id_usuario;
    private UUID id_estado;
    private UUID id_especialidad;
    /** Solicitud de origen, si el Requerimiento se generó desde una (ver
     * SolicitudServiceImpl.generarRequerimientoDesdeSolicitud); null si es
     * independiente. */
    private UUID id_solicitud;
    private String numeroRequerimiento;
    private Instant fecha_registro;
    private String descripcion;
    private String url_adjunto;
    /** Derivado (no persistido): fecha_registro + SLA fijo de Requerimiento. Ver SlaCalculator. */
    private Instant fecha_limite_despacho;
}
