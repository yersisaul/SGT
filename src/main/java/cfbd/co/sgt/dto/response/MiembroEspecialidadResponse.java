package cfbd.co.sgt.dto.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** Miembro de una especialidad. Sin email: no hace falta para asignar trabajo. */
@Getter
@Setter
public class MiembroEspecialidadResponse {
    private UUID id_usuario;
    private String nombres;
    private String apellidos;
    private Boolean es_responsable;
}
