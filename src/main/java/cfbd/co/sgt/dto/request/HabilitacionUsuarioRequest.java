package cfbd.co.sgt.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HabilitacionUsuarioRequest {
    @NotNull(message = "El campo habilitado es obligatorio")
    private Boolean habilitado;
}
