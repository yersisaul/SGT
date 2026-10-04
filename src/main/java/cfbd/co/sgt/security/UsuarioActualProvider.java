package cfbd.co.sgt.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Único punto para obtener la identidad y los permisos del usuario
 * autenticado desde el contexto de Spring Security (CLAUDE.md 6.1: nunca
 * desde un id enviado por el cliente). Los permisos provienen del JWT
 * validado por JwtAuthenticationFilter.
 */
@Component
@RequiredArgsConstructor
public class UsuarioActualProvider {

    private final UsuarioRepository usuarioRepository;

    public Usuario obtener() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    public boolean tienePermiso(String codigo) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (codigo.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
