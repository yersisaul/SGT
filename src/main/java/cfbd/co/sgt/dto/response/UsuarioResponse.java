package cfbd.co.sgt.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter 
public class UsuarioResponse {
    private UUID id_usuario;
    private String email;
    private String nombres;
    private String apellidos;
    private String url_img;
    private UUID id_rol;
    private String nombre_rol;    
}
