package cfbd.co.sgt.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter 
@Setter 
public class UsuarioRequest {
    private UUID id_rol;
    private String email;
    private String password;
    private String nombres;
    private String apellidos;
    private String url_img;
}

