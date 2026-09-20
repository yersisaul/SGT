package cfbd.co.sgt.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequerimientoRequest {
    private UUID id_usuario;
    private UUID id_estado;
    private UUID id_especialidad;
    private String descripcion;
    private String url_adjunto;
}
