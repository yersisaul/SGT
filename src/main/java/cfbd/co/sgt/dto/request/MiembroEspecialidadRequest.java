package cfbd.co.sgt.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Un miembro dentro de PUT /especialidades/{id}/miembros. */
@Getter
@Setter
public class MiembroEspecialidadRequest {
    @NotNull
    private UUID id_usuario;
    @NotNull
    private Boolean es_responsable;
}
