package cfbd.co.sgt.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Cuerpo de POST /ordenes/{id}/reasignar. */
@Getter
@Setter
public class ReasignarOrdenRequest {
    @NotNull
    private UUID id_usuario_nuevo;
    private String comentario;
}
