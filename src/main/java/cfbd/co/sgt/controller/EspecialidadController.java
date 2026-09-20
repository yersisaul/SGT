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

import cfbd.co.sgt.dto.request.EspecialidadRequest;
import cfbd.co.sgt.dto.response.EspecialidadResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.EspecialidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @PreAuthorize("hasAuthority('especialidad.read')")
    @GetMapping
    public List<EspecialidadResponse> getAllEspecialidades() {
        return especialidadService.listarEspecialidades();
    }

    @PreAuthorize("hasAuthority('especialidad.read')")
    @GetMapping("/{id}")
    public ResponseEntity<EspecialidadResponse> getEspecialidadById(@PathVariable UUID id) {
        EspecialidadResponse especialidad = especialidadService.buscarEspecialidadPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found"));
        return ResponseEntity.status(HttpStatus.OK).body(especialidad);
    }

    @PreAuthorize("hasAuthority('especialidad.create')")
    @PostMapping
    public ResponseEntity<EspecialidadResponse> createEspecialidad(@Valid @RequestBody EspecialidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(especialidadService.crearEspecialidad(request));
    }

    @PreAuthorize("hasAuthority('especialidad.update')")
    @PutMapping("/{id}")
    public ResponseEntity<EspecialidadResponse> updateEspecialidad(@Valid @RequestBody EspecialidadRequest request,
                                                                    @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(especialidadService.editarEspecialidad(request, id));
    }

    @PreAuthorize("hasAuthority('especialidad.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEspecialidad(@PathVariable UUID id) {
        especialidadService.eliminarEspecialidad(id);
        return ResponseEntity.noContent().build();
    }
}
