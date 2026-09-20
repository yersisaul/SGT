package cfbd.co.sgt.dto.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RolPermisoResponse {
    private UUID id_rol_permiso;
    private UUID id_rol;
    private String nombre_rol;
    private UUID id_permiso;
    private String codigo_permiso;
}
