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

import cfbd.co.sgt.dto.request.ActivoRequest;
import cfbd.co.sgt.dto.response.ActivoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.ActivoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/activos")
@RequiredArgsConstructor
public class ActivoController {

    private final ActivoService activoService;

    @PreAuthorize("hasAuthority('activo.read')")
    @GetMapping
    public List<ActivoResponse> getAllActivos() {
        return activoService.listarActivos();
    }

    @PreAuthorize("hasAuthority('activo.read')")
    @GetMapping("/{id}")
    public ResponseEntity<ActivoResponse> getActivoById(@PathVariable UUID id) {
        ActivoResponse activo = activoService.buscarActivoPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activo not found"));
        return ResponseEntity.status(HttpStatus.OK).body(activo);
    }

    @PreAuthorize("hasAuthority('activo.create')")
    @PostMapping
    public ResponseEntity<ActivoResponse> createActivo(@Valid @RequestBody ActivoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activoService.crearActivo(request));
    }

    @PreAuthorize("hasAuthority('activo.update')")
    @PutMapping("/{id}")
    public ResponseEntity<ActivoResponse> updateActivo(@Valid @RequestBody ActivoRequest request, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(activoService.editarActivo(request, id));
    }

    @PreAuthorize("hasAuthority('activo.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivo(@PathVariable UUID id) {
        activoService.eliminarActivo(id);
        return ResponseEntity.noContent().build();
    }
}
