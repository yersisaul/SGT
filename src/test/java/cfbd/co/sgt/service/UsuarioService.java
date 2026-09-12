package cfbd.co.sgt.service;

import cfbd.co.sgt.model.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import cfbd.co.sgt.dto.request.UsuarioRequest;
import cfbd.co.sgt.dto.response.UsuarioResponse;

public interface UsuarioService {
    Usuario crearUsuario(UsuarioRequest usuario);
    Usuario editarUsuario(UsuarioRequest usuario, UUID id);
    List<UsuarioResponse> listarUsuarios();
    Optional<UsuarioResponse> buscarUsuarioPorId(UUID id);
    Optional<UsuarioResponse> buscarUsuarioPorEmail(String email);
    void eliminarUsuario(UUID id);
}