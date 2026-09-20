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
    private String numeroRequerimiento;
    private Instant fecha_registro;
    private String descripcion;
    private String url_adjunto;
}
