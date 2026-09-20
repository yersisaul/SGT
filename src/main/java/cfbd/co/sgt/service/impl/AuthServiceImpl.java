package cfbd.co.sgt.service.impl;

import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cfbd.co.sgt.dto.AuthResponse;
import cfbd.co.sgt.dto.LoginRequest;
import cfbd.co.sgt.dto.LoginResponse;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.security.JwtService;
import cfbd.co.sgt.service.AuthService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Mismo mensaje para email inexistente y password incorrecta:
        // evita revelar si un email está registrado (enumeration).
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword_hash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        List<String> permisos = usuario.getRol().getRolPermisos().stream()
                .map(rp -> rp.getPermiso().getCodigo())
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
}
