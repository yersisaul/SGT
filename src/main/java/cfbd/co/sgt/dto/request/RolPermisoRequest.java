package cfbd.co.sgt.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RolPermisoRequest {
    private UUID id_rol;
    private UUID id_permiso;
}
