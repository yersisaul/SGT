package cfbd.co.sgt.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

@Getter 
@Setter 
public class ActivoRequest {
    /** Especialidad principal: con ella nace la Solicitud del activo. */
    private UUID id_especialidad;
    /** Todas las especialidades del activo (la principal se agrega aunque no venga). Opcional. */
    private List<UUID> ids_especialidad;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String ubicacion;
    private String url_img;
}
