package cfbd.co.sgt.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo de POST /ordenes/{id}/reasignar: mueve la OT a la cola de otra
 * especialidad (paso 12). Solo responsable de la especialidad actual o
 * Administrador (PRD D14).
 */
@Getter
@Setter
public class ReasignarOrdenRequest {
    @NotNull
    private UUID id_especialidad_destino;
    @NotBlank
    @Size(max = 1000)
    private String motivo;
}
