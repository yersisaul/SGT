package cfbd.co.sgt.dto.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstadoResponse {
    private UUID id_estado;
    private String nombre;
}
