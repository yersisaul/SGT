package cfbd.co.sgt.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cfbd.co.sgt.dto.request.AprobacionRequest;
import cfbd.co.sgt.dto.response.AprobacionResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.AprobacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Solo Create + Read: una aprobación es un registro de un evento de negocio
// ya ocurrido, no se edita ni se borra (ver decisión de alcance del CRUD).
@RestController
@RequestMapping("/api/aprobaciones")
@RequiredArgsConstructor
public class AprobacionController {

    private final AprobacionService aprobacionService;

    @PreAuthorize("hasAuthority('aprobacion.read')")
    @GetMapping
    public List<AprobacionResponse> getAllAprobaciones() {
        return aprobacionService.listarAprobaciones();
    }

    @PreAuthorize("hasAuthority('aprobacion.read')")
    @GetMapping("/{id}")
    public ResponseEntity<AprobacionResponse> getAprobacionById(@PathVariable UUID id) {
        AprobacionResponse aprobacion = aprobacionService.buscarAprobacionPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aprobacion not found"));
        return ResponseEntity.status(HttpStatus.OK).body(aprobacion);
    }

    // Permiso de negocio, no CRUD genérico: aprobar/rechazar transiciona el
    // Requerimiento y no debe confundirse con un simple registro de auditoría
    // (ver AprobacionServiceImpl.crearAprobacion).
    @PreAuthorize("hasAuthority('requerimiento.aprobar')")
    @PostMapping
    public ResponseEntity<AprobacionResponse> createAprobacion(@Valid @RequestBody AprobacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aprobacionService.crearAprobacion(request));
    }
}
