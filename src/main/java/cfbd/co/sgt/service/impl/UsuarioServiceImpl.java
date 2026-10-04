package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import cfbd.co.sgt.service.EmailNormalizador;
import cfbd.co.sgt.service.UsuarioService;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
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
    private final FileStorageService fileStorageService;

    @Override
    @Transactional    
    public UsuarioResponse crearUsuario(UsuarioRequest usuarioDTO) {
        String email = EmailNormalizador.normalizar(usuarioDTO.getEmail());
        if (usuarioRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }
        if (usuarioRepository.existsByNombres(usuarioDTO.getNombres())) {
            throw new DuplicateResourceException("Username already exists");
        }
        Rol rol = rolRepository.findById(usuarioDTO.getId_rol())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // La foto se gestiona exclusivamente vía subirImagen/eliminarImagen no se acepta desde este DTO.
        Usuario usuario = Usuario.builder()
                .email(email)
                .nombres(usuarioDTO.getNombres())
                .apellidos(usuarioDTO.getApellidos())
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

        usuario.setEmail(EmailNormalizador.normalizar(usuarioDTO.getEmail()));
        usuario.setNombres(usuarioDTO.getNombres());
        usuario.setApellidos(usuarioDTO.getApellidos());
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
    public List<UsuarioResponse> listarUsuariosConPermiso(String codigoPermiso) {
        return usuarioRepository.findByPermiso(codigoPermiso).stream()
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

    @Override
    @Transactional
    public UsuarioResponse subirImagen(UUID id, MultipartFile file) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String referenciaAnterior = usuario.getUrl_img();
        String nuevaReferencia = fileStorageService.store(file, TipoRecursoArchivo.USUARIOS, id);
        usuario.setUrl_img(nuevaReferencia);
        Usuario guardado = usuarioRepository.save(usuario);
        if (referenciaAnterior != null) {
            fileStorageService.delete(referenciaAnterior);
        }
        return usuarioMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerReferenciaImagen(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (usuario.getUrl_img() == null) {
            throw new ResourceNotFoundException("El usuario no tiene foto.");
        }
        return usuario.getUrl_img();
    }

    @Override
    @Transactional
    public UsuarioResponse eliminarImagen(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (usuario.getUrl_img() != null) {
            fileStorageService.delete(usuario.getUrl_img());
            usuario.setUrl_img(null);
            usuarioRepository.save(usuario);
        }
        return usuarioMapper.toResponse(usuario);
    }
}