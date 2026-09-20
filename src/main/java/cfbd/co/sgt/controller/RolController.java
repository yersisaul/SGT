package cfbd.co.sgt.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cfbd.co.sgt.dto.request.RolRequest;
import cfbd.co.sgt.dto.response.RolResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.mapper.RolMapper;
import cfbd.co.sgt.model.Rol;
import cfbd.co.sgt.service.RolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;
    private final RolMapper rolMapper;

    @PreAuthorize("hasAuthority('rol.read')")
    @GetMapping
    public List<RolResponse> getAllRoles() {
        return rolService.listarRoles().stream().map(rolMapper::toResponse).toList();
    }

    @PreAuthorize("hasAuthority('rol.read')")
    @GetMapping("/{id}")
    public ResponseEntity<RolResponse> getRolById(@PathVariable UUID id) {
        Rol rol = rolService.buscarRolPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol not found"));
        return ResponseEntity.status(HttpStatus.OK).body(rolMapper.toResponse(rol));
    }

    @PreAuthorize("hasAuthority('rol.create')")
    @PostMapping
    public ResponseEntity<RolResponse> createRol(@Valid @RequestBody RolRequest request) {
        Rol creado = rolService.crearRol(rolMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(rolMapper.toResponse(creado));
    }

    @PreAuthorize("hasAuthority('rol.update')")
    @PutMapping("/{id}")
    public ResponseEntity<RolResponse> updateRol(@Valid @RequestBody RolRequest request, @PathVariable UUID id) {
        Rol existente = rolService.buscarRolPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol not found"));
        Rol actualizado = rolService.editarRol(rolMapper.updateEntity(existente, request));
        return ResponseEntity.status(HttpStatus.OK).body(rolMapper.toResponse(actualizado));
    }

    @PreAuthorize("hasAuthority('rol.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRol(@PathVariable UUID id) {
        rolService.eliminarRol(id);
        return ResponseEntity.noContent().build();
    }
}
