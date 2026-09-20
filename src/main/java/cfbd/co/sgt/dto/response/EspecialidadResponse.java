package cfbd.co.sgt.dto.response;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EspecialidadResponse {
    private UUID id_especialidad;
    private String nombre;
    private String descripcion;
}
