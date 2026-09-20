package cfbd.co.sgt.dto.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RolResponse {
    private UUID id_rol;
    private String nombre;
    private String descripcion;
}
