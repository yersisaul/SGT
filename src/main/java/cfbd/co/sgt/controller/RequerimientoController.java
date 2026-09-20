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
import cfbd.co.sgt.dto.request.RequerimientoRequest;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.dto.response.RequerimientoResponse;
import cfbd.co.sgt.dto.response.ResumenEstadosResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.RequerimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/requerimientos")
@RequiredArgsConstructor
public class RequerimientoController {

    private final RequerimientoService requerimientoService;

    @PreAuthorize("hasAuthority('requerimiento.read')")
    @GetMapping
    public List<RequerimientoResponse> getAllRequerimientos() {
        return requerimientoService.listarRequerimientos();
    }

    @PreAuthorize("hasAuthority('requerimiento.read')")
    @GetMapping("/resumen-estados")
    public ResumenEstadosResponse getResumenPorEstado() {
        return requerimientoService.obtenerResumenPorEstado();
    }

    @PreAuthorize("hasAuthority('requerimiento.read')")
    @GetMapping("/{id}")
    public ResponseEntity<RequerimientoResponse> getRequerimientoById(@PathVariable UUID id) {
        RequerimientoResponse requerimiento = requerimientoService.buscarRequerimientoPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found"));
        return ResponseEntity.status(HttpStatus.OK).body(requerimiento);
    }

    @PreAuthorize("hasAuthority('requerimiento.create')")
    @PostMapping
    public ResponseEntity<RequerimientoResponse> createRequerimiento(@Valid @RequestBody RequerimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requerimientoService.crearRequerimiento(request));
    }

    @PreAuthorize("hasAuthority('requerimiento.update')")
    @PutMapping("/{id}")
    public ResponseEntity<RequerimientoResponse> updateRequerimiento(@Valid @RequestBody RequerimientoRequest request,
                                                                      @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(requerimientoService.editarRequerimiento(request, id));
    }

    @PreAuthorize("hasAuthority('requerimiento.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequerimiento(@PathVariable UUID id) {
        requerimientoService.eliminarRequerimiento(id);
        return ResponseEntity.noContent().build();
    }

    // Generar la Orden de Trabajo resultante de un Requerimiento ya
    // aprobado. Requiere requerimiento.generar_orden (distinto de
    // orden.create, que no se asigna a ningún rol) para que nadie pueda
    // saltarse la validación de aprobación llamando al CRUD genérico de Orden.
    @PreAuthorize("hasAuthority('requerimiento.generar_orden')")
    @PostMapping("/{id}/generar-orden")
    public ResponseEntity<OrdenResponse> generarOrdenDesdeRequerimiento(
            @PathVariable UUID id,
            @RequestBody(required = false) GenerarOrdenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requerimientoService.generarOrdenDesdeRequerimiento(id, request));
    }
}
