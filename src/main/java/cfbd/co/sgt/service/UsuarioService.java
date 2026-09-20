package cfbd.co.sgt.service;

import java.util.List;
import java.util.UUID;
import cfbd.co.sgt.dto.request.UsuarioRequest;
import cfbd.co.sgt.dto.response.UsuarioResponse;

public interface UsuarioService {
    UsuarioResponse crearUsuario(UsuarioRequest usuario);
    UsuarioResponse editarUsuario(UsuarioRequest usuario, UUID id);
    List<UsuarioResponse> listarUsuarios();
    UsuarioResponse buscarUsuarioPorId(UUID id);
    UsuarioResponse buscarUsuarioPorEmail(String email);
    void eliminarUsuario(UUID id);
}