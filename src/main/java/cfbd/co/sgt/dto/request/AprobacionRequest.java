package cfbd.co.sgt.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AprobacionRequest {
    private UUID id_requerimiento;
    private Boolean aprobado;
    private String comentario;
    private String url_adjunto;
}
