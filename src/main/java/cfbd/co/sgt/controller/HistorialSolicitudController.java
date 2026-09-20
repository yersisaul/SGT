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

import cfbd.co.sgt.dto.request.HistorialSolicitudRequest;
import cfbd.co.sgt.dto.response.HistorialSolicitudResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.HistorialSolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Solo Create + Read: registro de auditoría, no se edita ni se borra.
@RestController
@RequestMapping("/api/historial-solicitudes")
@RequiredArgsConstructor
public class HistorialSolicitudController {

    private final HistorialSolicitudService historialSolicitudService;

    @PreAuthorize("hasAuthority('historial_solicitud.read')")
    @GetMapping
    public List<HistorialSolicitudResponse> getAllHistoriales() {
        return historialSolicitudService.listarHistoriales();
    }

    @PreAuthorize("hasAuthority('historial_solicitud.read')")
    @GetMapping("/{id}")
    public ResponseEntity<HistorialSolicitudResponse> getHistorialById(@PathVariable UUID id) {
        HistorialSolicitudResponse historial = historialSolicitudService.buscarHistorialPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Historial not found"));
        return ResponseEntity.status(HttpStatus.OK).body(historial);
    }

    @PreAuthorize("hasAuthority('historial_solicitud.create')")
    @PostMapping
    public ResponseEntity<HistorialSolicitudResponse> createHistorial(
            @Valid @RequestBody HistorialSolicitudRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(historialSolicitudService.crearHistorial(request));
    }
}
