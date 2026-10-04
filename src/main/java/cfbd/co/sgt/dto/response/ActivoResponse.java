package cfbd.co.sgt.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

@Getter 
@Setter 
public class ActivoResponse {
    private UUID id_activo;
    /** Especialidad principal. */
    private UUID id_especialidad;
    /** Todas las especialidades del activo, incluida la principal. */
    private List<UUID> ids_especialidad;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String ubicacion;
    private String url_img;
}
