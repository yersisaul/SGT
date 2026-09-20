package cfbd.co.sgt.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import cfbd.co.sgt.dto.response.UsuarioResponse;
import cfbd.co.sgt.dto.request.UsuarioRequest;
import cfbd.co.sgt.service.UsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @PreAuthorize("hasAuthority('usuario.read')")
    @GetMapping
    public List<UsuarioResponse> getAllUsers() {
        List<UsuarioResponse> usuarios = usuarioService.listarUsuarios();
        return usuarios;
    }

    @PreAuthorize("hasAuthority('usuario.read')")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.buscarUsuarioPorId(id));
    }

    @PreAuthorize("hasAuthority('usuario.create')")
    @PostMapping
    public ResponseEntity<UsuarioResponse> createUser(@Valid @RequestBody UsuarioRequest usuarioDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearUsuario(usuarioDTO));
    }

    @PreAuthorize("hasAuthority('usuario.update')")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> updateUser(@Valid @RequestBody UsuarioRequest usuarioDTO, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.editarUsuario(usuarioDTO, id));
    }

    @PreAuthorize("hasAuthority('usuario.delete')")
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable UUID id) {
        usuarioService.eliminarUsuario(id);
    }
}
