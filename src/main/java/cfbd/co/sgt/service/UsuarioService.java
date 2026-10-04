package cfbd.co.sgt.service;

import java.util.List;
import java.util.UUID;
import cfbd.co.sgt.dto.request.UsuarioRequest;
import cfbd.co.sgt.dto.response.UsuarioResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UsuarioService {
    UsuarioResponse crearUsuario(UsuarioRequest usuario);
    UsuarioResponse editarUsuario(UsuarioRequest usuario, UUID id);
    List<UsuarioResponse> listarUsuarios();
    /** Usuarios cuyo rol tiene el permiso indicado (p. ej. orden.tomar = pueden integrar un equipo). */
    List<UsuarioResponse> listarUsuariosConPermiso(String codigoPermiso);
    UsuarioResponse buscarUsuarioPorId(UUID id);
    UsuarioResponse buscarUsuarioPorEmail(String email);
    void eliminarUsuario(UUID id);

    UsuarioResponse subirImagen(UUID id, MultipartFile file);
    String obtenerReferenciaImagen(UUID id);
    UsuarioResponse eliminarImagen(UUID id);
}
