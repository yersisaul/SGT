package cfbd.co.sgt.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo de POST /solicitudes/{id}/generar-orden y
 * POST /requerimientos/{id}/generar-orden. La OT se asigna a la cola de la
 * especialidad elegida (PRD D3/D16), nunca a una persona: no hay ejecutor
 * en el body. El estado inicial y el número se derivan en el Service.
 */
@Getter
@Setter
public class GenerarOrdenRequest {
    @NotNull
    private UUID id_especialidad;
    @Size(max = 255)
    private String comentario;
}
