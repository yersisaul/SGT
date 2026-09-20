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

import cfbd.co.sgt.dto.request.EstadoRequest;
import cfbd.co.sgt.dto.response.EstadoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.EstadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/estados")
@RequiredArgsConstructor
public class EstadoController {

    private final EstadoService estadoService;

    @PreAuthorize("hasAuthority('estado.read')")
    @GetMapping
    public List<EstadoResponse> getAllEstados() {
        return estadoService.listarEstados();
    }

    @PreAuthorize("hasAuthority('estado.read')")
    @GetMapping("/{id}")
    public ResponseEntity<EstadoResponse> getEstadoById(@PathVariable UUID id) {
        EstadoResponse estado = estadoService.buscarEstadoPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado not found"));
        return ResponseEntity.status(HttpStatus.OK).body(estado);
    }

    @PreAuthorize("hasAuthority('estado.create')")
    @PostMapping
    public ResponseEntity<EstadoResponse> createEstado(@Valid @RequestBody EstadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estadoService.crearEstado(request));
    }

    @PreAuthorize("hasAuthority('estado.update')")
    @PutMapping("/{id}")
    public ResponseEntity<EstadoResponse> updateEstado(@Valid @RequestBody EstadoRequest request, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(estadoService.editarEstado(request, id));
    }

    @PreAuthorize("hasAuthority('estado.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEstado(@PathVariable UUID id) {
        estadoService.eliminarEstado(id);
        return ResponseEntity.noContent().build();
    }
}
