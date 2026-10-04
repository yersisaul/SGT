package cfbd.co.sgt.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Cuerpo de POST /ordenes/{id}/asignar: el responsable asigna la OT a un miembro. */
@Getter
@Setter
public class AsignarOrdenRequest {
    @NotNull
    private UUID id_usuario;
}
