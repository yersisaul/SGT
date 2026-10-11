package cfbd.co.sgt.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Publica o despublica un registro del sitio web público. */
@Getter
@Setter
public class PublicacionWebRequest {
    @NotNull(message = "El campo publicado es obligatorio")
    private Boolean publicado;
}
