package cfbd.co.sgt.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
import java.time.Instant;

@Getter 
@Setter 
public class OrdenResponse {
    public UUID id_orden;
    /** Ejecutor de Operaciones responsable de la Orden (Orden.usuario). */
    private UUID id_usuario;
    private UUID id_estado;
    private UUID id_especialidad;
    private UUID id_solicitud;
    private UUID id_requerimiento;
    private String numeroOrden;
    private Instant fecha_registro;
    private Instant fecha_cierre;
    private String url_adjunto;
}

