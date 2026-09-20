package cfbd.co.sgt.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter 
@Setter 
public class OrdenRequest {
    private UUID id_usuario;
    private UUID id_estado;
    private UUID id_especialidad;
    private UUID id_solicitud;
    private UUID id_requerimiento;
    private String url_adjunto;
}
