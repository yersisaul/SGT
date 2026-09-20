package cfbd.co.sgt.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistorialOrdenRequest {
    private UUID id_orden;
    private UUID id_estado_anterior;
    private UUID id_estado_nuevo;
    private String comentario;
}
