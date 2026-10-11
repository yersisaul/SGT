package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** Cliente de referencia para el panel de administración (incluye no publicados y auditoría). */
@Getter
@Setter
public class WebClienteReferenciaResponse {
    private UUID id_cliente_referencia;
    private String nombre;
    private String sector;
    private String url_logo;
    private String url_sitio;
    private int orden;
    private boolean publicado;
    private Instant fecha_creacion;
    private Instant fecha_actualizacion;
    private String nombre_usuario_actualiza;
}
