package cfbd.co.sgt.mapper;

import cfbd.co.sgt.dto.response.UsuarioResponse;
import cfbd.co.sgt.model.Usuario;
import org.springframework.stereotype.Component;

@Component 
public class UsuarioMapper {
    public UsuarioResponse toResponse(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        UsuarioResponse response = new UsuarioResponse();
        response.setId_usuario(usuario.getId_usuario());
        response.setEmail(usuario.getEmail());
        response.setNombres(usuario.getNombres());
        response.setApellidos(usuario.getApellidos());
        response.setUrl_img(usuario.getUrl_img() != null ? "/api/archivos/usuarios/" + usuario.getId_usuario() : null);
        response.setId_rol(usuario.getRol().getId_rol());
        response.setNombre_rol(usuario.getRol().getNombre());
        return response;
    }
}
