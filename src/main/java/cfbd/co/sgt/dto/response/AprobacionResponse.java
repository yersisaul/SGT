package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AprobacionResponse {
    private UUID id_aprobacion;
    private UUID id_requerimiento;
    private UUID id_usuario;
    private Boolean aprobado;
    private String comentario;
    private Instant fecha_aprobacion;
    private String url_adjunto;
}
