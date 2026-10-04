package cfbd.co.sgt.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo de POST /ordenes/{id}/verificar (paso 11). Si corresponde=false el
 * motivo es obligatorio (se valida en el Service) y la OT queda "Devuelta".
 */
@Getter
@Setter
public class VerificarOrdenRequest {
    @NotNull
    private Boolean corresponde;
    @Size(max = 1000)
    private String motivo;
}
