package cfbd.co.sgt.dto.request;

import java.time.Instant;

import cfbd.co.sgt.model.EstadoNovedad;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Cambio de estado de una novedad (permiso web_novedad.publish). Al publicar,
 * fecha_publicacion opcional en ISO-8601 (2026-10-15T14:00:00Z): futura =
 * programada; sin ella se conserva la anterior o, si no tenía, ahora.
 */
@Getter
@Setter
public class EstadoNovedadRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoNovedad estado;

    private Instant fecha_publicacion;
}
