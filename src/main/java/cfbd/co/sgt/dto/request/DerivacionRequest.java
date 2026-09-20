package cfbd.co.sgt.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DerivacionRequest {
    private UUID id_solicitud;
    private UUID id_usuario_destino;
    private String motivo;
    private String observacion;
}
