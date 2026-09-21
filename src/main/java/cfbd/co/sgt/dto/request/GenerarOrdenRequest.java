package cfbd.co.sgt.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo de POST /solicitudes/{id}/generar-orden y
 * POST /requerimientos/{id}/generar-orden. Especialidad, estado inicial y
 * número se derivan en el Service a partir de la Solicitud/Requerimiento de
 * origen; el usuario autenticado nunca se usa como ejecutor. El ejecutor de
 * Operaciones sí lo elige el cliente (id_usuario_ejecutor), y es obligatorio:
 * no se puede generar una OT sin ejecutor asignado.
 */
@Getter
@Setter
public class GenerarOrdenRequest {
    @NotNull
    private UUID id_usuario_ejecutor;
    private String comentario;
    private String url_adjunto;
}
