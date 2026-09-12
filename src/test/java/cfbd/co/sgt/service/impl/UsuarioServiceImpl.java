package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import cfbd.co.sgt.service.UsuarioService;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.repository.RolRepository;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.model.Rol;
import cfbd.co.sgt.dto.request.UsuarioRequest;
import cfbd.co.sgt.dto.response.UsuarioResponse;
import java.util.UUID;
import cfbd.co.sgt.security.SecurityConfig;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service 
@Transactional 
public class UsuarioServiceImpl implements UsuarioService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired 
    private RolRepository rolRepository;

    @Autowired
    private SecurityConfig securityConfig;

    @Override
    public Usuario crearUsuario(UsuarioRequest usuarioDTO) {
        Rol rol = rolRepository.findById(usuarioDTO.getId_rol()).orElse(null);
        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setEmail(usuarioDTO.getEmail());
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String password_has = passwordEncoder.encode(usuarioDTO.getPassword());
        usuario.setPassword_hash(password_has);
        usuario.setNombres(usuarioDTO.getNombres());
        usuario.setApellidos(usuarioDTO.getApellidos());
        usuario.setUrl_img(usuarioDTO.getUrl_img());
        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario editarUsuario(UsuarioRequest usuarioDTO, UUID id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario != null) {
            Rol rol = rolRepository.findById(usuarioDTO.getId_rol()).orElse(null);
            usuario.setRol(rol);
            usuario.setEmail(usuarioDTO.getEmail());
            PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
            String password_has = passwordEncoder.encode(usuarioDTO.getPassword());
            usuario.setPassword_hash(password_has);
            usuario.setNombres(usuarioDTO.getNombres());
            usuario.setApellidos(usuarioDTO.getApellidos());
            usuario.setUrl_img(usuarioDTO.getUrl_img());
            return usuarioRepository.save(usuario);
        }
        return null;
    }

    @Override
    public List<UsuarioResponse> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        // Convertir la lista de usuarios a una lista de UsuarioResponse
        return usuarios.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

    }

    @Override
    public Optional<UsuarioResponse> buscarUsuarioPorId(UUID id) {
        return usuarioRepository.findById(id).map(this::convertToResponse);
    }

    @Override
    public Optional<UsuarioResponse> buscarUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email).map(this::convertToResponse);
    }

    private UsuarioResponse convertToResponse(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setId_usuario(usuario.getId_usuario());
        response.setEmail(usuario.getEmail());
        response.setNombres(usuario.getNombres());
        response.setApellidos(usuario.getApellidos());
        response.setUrl_img(usuario.getUrl_img());
        response.setId_rol(usuario.getRol().getId_rol());
        response.setNombre_rol(usuario.getRol().getNombre());
        return response;
    }

    @Override
    public void eliminarUsuario(UUID id) {
        usuarioRepository.deleteById(id);
    }
}