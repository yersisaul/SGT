package cfbd.co.sgt.dto.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** Carga de un miembro del equipo: OT abiertas que tiene asignadas en la especialidad. */
@Getter
@Setter
public class CargaMiembroResponse {
    private UUID id_usuario;
    private String nombres;
    private String apellidos;
    private Boolean es_responsable;
    private long ordenes_abiertas;
}
