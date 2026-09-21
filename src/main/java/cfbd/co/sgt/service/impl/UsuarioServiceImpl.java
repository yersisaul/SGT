package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import cfbd.co.sgt.service.UsuarioService;
import org.springframework.transaction.annotation.Transactional;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.repository.RolRepository;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.model.Rol;
import cfbd.co.sgt.dto.request.UsuarioRequest;
import cfbd.co.sgt.dto.response.UsuarioResponse;
import cfbd.co.sgt.mapper.UsuarioMapper;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.exception.DuplicateResourceException;

@Service 
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Override
    @Transactional    
    public UsuarioResponse crearUsuario(UsuarioRequest usuarioDTO) {
        if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }
        if (usuarioRepository.existsByNombres(usuarioDTO.getNombres())) {
            throw new DuplicateResourceException("Username already exists");
        }
        Rol rol = rolRepository.findById(usuarioDTO.getId_rol())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        Usuario usuario = Usuario.builder()
                .email(usuarioDTO.getEmail())
                .nombres(usuarioDTO.getNombres())
                .apellidos(usuarioDTO.getApellidos())                
                .url_img(usuarioDTO.getUrl_img())
                .rol(rol)
                .password_hash(passwordEncoder.encode(usuarioDTO.getPassword()))
                .build();
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse editarUsuario(UsuarioRequest usuarioDTO, UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setNombres(usuarioDTO.getNombres());
        usuario.setApellidos(usuarioDTO.getApellidos());
        usuario.setUrl_img(usuarioDTO.getUrl_img());
        usuario.setRol(rolRepository.findById(usuarioDTO.getId_rol())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found")));

        if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isBlank()) {
            usuario.setPassword_hash(passwordEncoder.encode(usuarioDTO.getPassword()));
        }
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        // Convertir la lista de usuarios a una lista de UsuarioResponse
        return usuarios.stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuariosPorRol(String nombreRol) {
        return usuarioRepository.findByRolNombre(nombreRol).stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse buscarUsuarioPorId(UUID id) {
        return usuarioRepository.findById(id).map(usuarioMapper::toResponse).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse buscarUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email).map(usuarioMapper::toResponse).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public void eliminarUsuario(UUID id) {
        usuarioRepository.deleteById(id);
    }
}