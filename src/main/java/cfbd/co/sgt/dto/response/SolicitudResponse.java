package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SolicitudResponse {
    private UUID id_solicitud;
    private UUID id_usuario;
    /** Nombre del solicitante, para mostrarlo sin exigir usuario.read. */
    private String nombre_usuario;
    private UUID id_activo;
    private UUID id_estado;
    private UUID id_especialidad;
    private String numeroSolicitud;
    private String prioridad;
    private Instant fecha_registro;
    private String descripcion;
    private String url_adjunto;
    /** Derivado (no persistido): fecha_registro + SLA según prioridad. Ver SlaCalculator. */
    private Instant fecha_limite_despacho;
}
