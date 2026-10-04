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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cfbd.co.sgt.dto.request.HistorialOrdenRequest;
import cfbd.co.sgt.dto.response.HistorialOrdenResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.HistorialOrdenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Solo Create + Read: registro de auditoría, no se edita ni se borra.
@RestController
@RequestMapping("/api/historial-ordenes")
@RequiredArgsConstructor
public class HistorialOrdenController {

    private final HistorialOrdenService historialOrdenService;

    @PreAuthorize("hasAuthority('historial_orden.read')")
    @GetMapping
    public List<HistorialOrdenResponse> getAllHistoriales(
            @RequestParam(name = "id_orden", required = false) UUID idPadre) {
        return historialOrdenService.listarHistoriales(idPadre);
    }

    @PreAuthorize("hasAuthority('historial_orden.read')")
    @GetMapping("/{id}")
    public ResponseEntity<HistorialOrdenResponse> getHistorialById(@PathVariable UUID id) {
        HistorialOrdenResponse historial = historialOrdenService.buscarHistorialPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Historial not found"));
        return ResponseEntity.status(HttpStatus.OK).body(historial);
    }

    @PreAuthorize("hasAuthority('historial_orden.create')")
    @PostMapping
    public ResponseEntity<HistorialOrdenResponse> createHistorial(@Valid @RequestBody HistorialOrdenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(historialOrdenService.crearHistorial(request));
    }
}
