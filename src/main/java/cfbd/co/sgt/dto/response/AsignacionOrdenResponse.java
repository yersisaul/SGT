package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** Evento de asignación de una OT (GET /ordenes/{id}/asignaciones). */
@Getter
@Setter
public class AsignacionOrdenResponse {
    private UUID id_asignacion_orden;
    private UUID id_orden;
    private String tipo;
    private UUID id_especialidad_origen;
    private UUID id_especialidad_destino;
    private UUID id_usuario_origen;
    private UUID id_usuario_destino;
    private UUID id_usuario_actor;
    private String nombre_actor;
    private String motivo;
    private Instant fecha;
}
