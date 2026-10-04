package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistorialOrdenResponse {
    private UUID id_historial_orden;
    private UUID id_orden;
    private UUID id_usuario;
    /** Quién hizo el cambio (sin exigir usuario.read para mostrarlo). */
    private String nombre_usuario;
    private UUID id_estado_anterior;
    private UUID id_estado_nuevo;
    private Instant fecha;
    private String comentario;
}
