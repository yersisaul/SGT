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

import cfbd.co.sgt.dto.request.PermisoRequest;
import cfbd.co.sgt.dto.response.PermisoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.mapper.PermisoMapper;
import cfbd.co.sgt.model.Permiso;
import cfbd.co.sgt.service.PermisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/permisos")
@RequiredArgsConstructor
public class PermisoController {

    private final PermisoService permisoService;
    private final PermisoMapper permisoMapper;

    @PreAuthorize("hasAuthority('permiso.read')")
    @GetMapping
    public List<PermisoResponse> getAllPermisos() {
        return permisoService.listarPermisos().stream().map(permisoMapper::toResponse).toList();
    }

    @PreAuthorize("hasAuthority('permiso.read')")
    @GetMapping("/{id}")
    public ResponseEntity<PermisoResponse> getPermisoById(@PathVariable UUID id) {
        Permiso permiso = permisoService.buscarPermisoPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso not found"));
        return ResponseEntity.status(HttpStatus.OK).body(permisoMapper.toResponse(permiso));
    }

    @PreAuthorize("hasAuthority('permiso.create')")
    @PostMapping
    public ResponseEntity<PermisoResponse> createPermiso(@Valid @RequestBody PermisoRequest request) {
        Permiso creado = permisoService.crearPermiso(permisoMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(permisoMapper.toResponse(creado));
    }

    @PreAuthorize("hasAuthority('permiso.update')")
    @PutMapping("/{id}")
    public ResponseEntity<PermisoResponse> updatePermiso(@Valid @RequestBody PermisoRequest request, @PathVariable UUID id) {
        Permiso existente = permisoService.buscarPermisoPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso not found"));
        Permiso actualizado = permisoService.editarPermiso(permisoMapper.updateEntity(existente, request));
        return ResponseEntity.status(HttpStatus.OK).body(permisoMapper.toResponse(actualizado));
    }

    @PreAuthorize("hasAuthority('permiso.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermiso(@PathVariable UUID id) {
        permisoService.eliminarPermiso(id);
        return ResponseEntity.noContent().build();
    }
}
