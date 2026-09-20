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
    private UUID id_activo;
    private UUID id_estado;
    private UUID id_especialidad;
    private String numeroSolicitud;
    private String prioridad;
    private Instant fecha_registro;
    private String descripcion;
    private String url_adjunto;
}
