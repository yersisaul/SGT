package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cfbd.co.sgt.dto.AuthResponse;
import cfbd.co.sgt.dto.LoginRequest;
import cfbd.co.sgt.dto.LoginResponse;
import cfbd.co.sgt.dto.RegistroClienteRequest;
import cfbd.co.sgt.model.Rol;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.RolRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.security.JwtService;
import cfbd.co.sgt.security.Permisos;
import cfbd.co.sgt.service.EmailNormalizador;
import cfbd.co.sgt.service.AuthService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** Rol que recibe todo auto-registro desde el sitio web; nunca lo elige el cliente. */
    private static final String ROL_REGISTRO_PUBLICO = "Cliente";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Mismo mensaje para email inexistente y password incorrecta:
        // evita revelar si un email está registrado (enumeration).
        Usuario usuario = usuarioRepository.findByEmail(EmailNormalizador.normalizar(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword_hash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        // Solo se informa después de validar la contraseña: así no revela
        // qué emails están registrados.
        if (!usuario.isHabilitado()) {
            throw new DisabledException("Cuenta pendiente de habilitación por un administrador");
        }

        List<String> permisos = Stream.concat(
                        usuario.getRol().getRolPermisos().stream().map(rp -> rp.getPermiso().getCodigo()),
                        Permisos.LECTURA_BASE.stream())
                .distinct()
                .toList();

        String accessToken = jwtService.generarToken(
                usuario.getId_usuario(), usuario.getEmail(), usuario.getRol().getNombre(), permisos);

        LoginResponse loginResponse = new LoginResponse(
                usuario.getId_usuario(),
                usuario.getEmail(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getRol().getNombre());

        return new AuthResponse(accessToken, null, loginResponse);
    }

    @Override
    @Transactional
    public void registrarCliente(RegistroClienteRequest request) {
        String email = EmailNormalizador.normalizar(request.email());
        // El hash se calcula siempre, antes de la verificación, para que un
        // email ya registrado no responda más rápido que uno nuevo.
        String passwordHash = passwordEncoder.encode(request.password());

        // Email ya registrado: misma respuesta que un registro nuevo (no se
        // revela qué emails existen) y la cuenta existente no se toca.
        if (usuarioRepository.existsByEmail(email)) {
            return;
        }

        Rol rolCliente = rolRepository.findByNombre(ROL_REGISTRO_PUBLICO)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + ROL_REGISTRO_PUBLICO));

        usuarioRepository.save(Usuario.builder()
                .email(email)
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .rol(rolCliente)
                .password_hash(passwordHash)
                .habilitado(false)
                .build());
    }
}
