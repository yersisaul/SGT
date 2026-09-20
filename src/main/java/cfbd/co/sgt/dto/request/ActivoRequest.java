package cfbd.co.sgt.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter 
@Setter 
public class ActivoRequest {
    private UUID id_especialidad;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String ubicacion;
    private String url_img;
}
