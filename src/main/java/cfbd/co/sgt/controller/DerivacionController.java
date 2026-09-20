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

import cfbd.co.sgt.dto.request.DerivacionRequest;
import cfbd.co.sgt.dto.response.DerivacionResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.DerivacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Solo Create + Read: una derivación es un registro de un evento de negocio
// ya ocurrido, no se edita ni se borra (ver decisión de alcance del CRUD).
@RestController
@RequestMapping("/api/derivaciones")
@RequiredArgsConstructor
public class DerivacionController {

    private final DerivacionService derivacionService;

    @PreAuthorize("hasAuthority('derivacion.read')")
    @GetMapping
    public List<DerivacionResponse> getAllDerivaciones() {
        return derivacionService.listarDerivaciones();
    }

    @PreAuthorize("hasAuthority('derivacion.read')")
    @GetMapping("/{id}")
    public ResponseEntity<DerivacionResponse> getDerivacionById(@PathVariable UUID id) {
        DerivacionResponse derivacion = derivacionService.buscarDerivacionPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Derivacion not found"));
        return ResponseEntity.status(HttpStatus.OK).body(derivacion);
    }

    @PreAuthorize("hasAuthority('derivacion.create')")
    @PostMapping
    public ResponseEntity<DerivacionResponse> createDerivacion(@Valid @RequestBody DerivacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(derivacionService.crearDerivacion(request));
    }
}
