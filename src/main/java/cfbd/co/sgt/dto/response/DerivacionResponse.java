package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DerivacionResponse {
    private UUID id_derivacion;
    private UUID id_solicitud;
    private UUID id_usuario_origen;
    private UUID id_usuario_destino;
    private Instant fecha_derivacion;
    private String motivo;
    private String observacion;
}
