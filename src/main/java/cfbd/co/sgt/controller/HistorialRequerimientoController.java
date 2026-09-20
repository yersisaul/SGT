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

import cfbd.co.sgt.dto.request.HistorialRequerimientoRequest;
import cfbd.co.sgt.dto.response.HistorialRequerimientoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.HistorialRequerimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Solo Create + Read: registro de auditoría, no se edita ni se borra.
@RestController
@RequestMapping("/api/historial-requerimientos")
@RequiredArgsConstructor
public class HistorialRequerimientoController {

    private final HistorialRequerimientoService historialRequerimientoService;

    @PreAuthorize("hasAuthority('historial_requerimiento.read')")
    @GetMapping
    public List<HistorialRequerimientoResponse> getAllHistoriales() {
        return historialRequerimientoService.listarHistoriales();
    }

    @PreAuthorize("hasAuthority('historial_requerimiento.read')")
    @GetMapping("/{id}")
    public ResponseEntity<HistorialRequerimientoResponse> getHistorialById(@PathVariable UUID id) {
        HistorialRequerimientoResponse historial = historialRequerimientoService.buscarHistorialPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Historial not found"));
        return ResponseEntity.status(HttpStatus.OK).body(historial);
    }

    @PreAuthorize("hasAuthority('historial_requerimiento.create')")
    @PostMapping
    public ResponseEntity<HistorialRequerimientoResponse> createHistorial(
            @Valid @RequestBody HistorialRequerimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(historialRequerimientoService.crearHistorial(request));
    }
}
