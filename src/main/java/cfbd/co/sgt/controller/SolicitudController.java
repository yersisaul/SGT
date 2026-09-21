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

import cfbd.co.sgt.dto.request.GenerarOrdenRequest;
import cfbd.co.sgt.dto.request.SolicitudRequest;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.dto.response.ResumenEstadosResponse;
import cfbd.co.sgt.dto.response.SolicitudResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.SolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    @PreAuthorize("hasAuthority('solicitud.read')")
    @GetMapping
    public List<SolicitudResponse> getAllSolicitudes() {
        return solicitudService.listarSolicitudes();
    }

    @PreAuthorize("hasAuthority('solicitud.read')")
    @GetMapping("/resumen-estados")
    public ResumenEstadosResponse getResumenPorEstado() {
        return solicitudService.obtenerResumenPorEstado();
    }

    @PreAuthorize("hasAuthority('solicitud.read')")
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudResponse> getSolicitudById(@PathVariable UUID id) {
        SolicitudResponse solicitud = solicitudService.buscarSolicitudPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found"));
        return ResponseEntity.status(HttpStatus.OK).body(solicitud);
    }

    @PreAuthorize("hasAuthority('solicitud.create')")
    @PostMapping
    public ResponseEntity<SolicitudResponse> createSolicitud(@Valid @RequestBody SolicitudRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.crearSolicitud(request));
    }

    @PreAuthorize("hasAuthority('solicitud.update')")
    @PutMapping("/{id}")
    public ResponseEntity<SolicitudResponse> updateSolicitud(@Valid @RequestBody SolicitudRequest request,
                                                              @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(solicitudService.editarSolicitud(request, id));
    }

    @PreAuthorize("hasAuthority('solicitud.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSolicitud(@PathVariable UUID id) {
        solicitudService.eliminarSolicitud(id);
        return ResponseEntity.noContent().build();
    }

    // Generar una Orden de Trabajo directamente desde una Solicitud "bajo
    // contrato". Permiso de negocio distinto de solicitud.update/orden.create
    // (CLAUDE.md 5.3): evita que el CRUD genérico de Orden permita saltarse
    // el flujo de clasificación del Despachador.
    @PreAuthorize("hasAuthority('solicitud.generar_orden')")
    @PostMapping("/{id}/generar-orden")
    public ResponseEntity<OrdenResponse> generarOrdenDesdeSolicitud(
            @PathVariable UUID id,
            @Valid @RequestBody GenerarOrdenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solicitudService.generarOrdenDesdeSolicitud(id, request));
    }
}
