package cfbd.co.sgt.dto.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermisoResponse {
    private UUID id_permiso;
    private String codigo;
    private String descripcion;
}
