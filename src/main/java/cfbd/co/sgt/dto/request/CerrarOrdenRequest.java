package cfbd.co.sgt.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Cuerpo (opcional) de POST /ordenes/{id}/cerrar. */
@Getter
@Setter
public class CerrarOrdenRequest {
    private String comentario;
}
