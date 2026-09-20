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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cfbd.co.sgt.dto.request.RolPermisoRequest;
import cfbd.co.sgt.dto.response.RolPermisoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.RolPermisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Create (asignar) + Read + Delete (revocar): no existe "editar" una
// asignación rol-permiso, se revoca y se vuelve a asignar.
@RestController
@RequestMapping("/api/rol-permisos")
@RequiredArgsConstructor
public class RolPermisoController {

    private final RolPermisoService rolPermisoService;

    @PreAuthorize("hasAuthority('rolpermiso.read')")
    @GetMapping
    public List<RolPermisoResponse> getAllRolPermisos() {
        return rolPermisoService.listarRolPermisos();
    }

    @PreAuthorize("hasAuthority('rolpermiso.read')")
    @GetMapping("/{id}")
    public ResponseEntity<RolPermisoResponse> getRolPermisoById(@PathVariable UUID id) {
        RolPermisoResponse rolPermiso = rolPermisoService.buscarRolPermisoPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("RolPermiso not found"));
        return ResponseEntity.status(HttpStatus.OK).body(rolPermiso);
    }

    @PreAuthorize("hasAuthority('rolpermiso.create')")
    @PostMapping
    public ResponseEntity<RolPermisoResponse> asignarPermiso(@Valid @RequestBody RolPermisoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolPermisoService.asignarPermiso(request));
    }

    @PreAuthorize("hasAuthority('rolpermiso.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revocarPermiso(@PathVariable UUID id) {
        rolPermisoService.revocarPermiso(id);
        return ResponseEntity.noContent().build();
    }
}
