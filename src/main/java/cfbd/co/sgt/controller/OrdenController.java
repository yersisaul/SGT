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

import cfbd.co.sgt.dto.request.CerrarOrdenRequest;
import cfbd.co.sgt.dto.request.OrdenRequest;
import cfbd.co.sgt.dto.request.ReasignarOrdenRequest;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.OrdenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
public class OrdenController {

    private final OrdenService ordenService;

    @PreAuthorize("hasAuthority('orden.read')")
    @GetMapping
    public List<OrdenResponse> getAllOrdenes() {
        return ordenService.listarOrdenes();
    }

    @PreAuthorize("hasAuthority('orden.read')")
    @GetMapping("/{id}")
    public ResponseEntity<OrdenResponse> getOrdenById(@PathVariable UUID id) {
        OrdenResponse orden = ordenService.buscarOrdenPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found"));
        return ResponseEntity.status(HttpStatus.OK).body(orden);
    }

    @PreAuthorize("hasAuthority('orden.create')")
    @PostMapping
    public ResponseEntity<OrdenResponse> createOrden(@Valid @RequestBody OrdenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenService.crearOrden(request));
    }

    @PreAuthorize("hasAuthority('orden.update')")
    @PutMapping("/{id}")
    public ResponseEntity<OrdenResponse> updateOrden(@Valid @RequestBody OrdenRequest request, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(ordenService.editarOrden(request, id));
    }

    @PreAuthorize("hasAuthority('orden.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrden(@PathVariable UUID id) {
        ordenService.eliminarOrden(id);
        return ResponseEntity.noContent().build();
    }

    // Cierre de OT: operación de negocio específica (fecha_cierre + estado
    // final), separada del PUT genérico (CLAUDE.md 5.3).
    @PreAuthorize("hasAuthority('orden.cerrar')")
    @PostMapping("/{id}/cerrar")
    public ResponseEntity<OrdenResponse> cerrarOrden(@PathVariable UUID id,
                                                      @RequestBody(required = false) CerrarOrdenRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(ordenService.cerrarOrden(id, request));
    }

    // Reasignar el ejecutor de una Orden: la puede pedir el ejecutor actual o
    // un Administrador (validado en el Service, no solo aquí).
    @PreAuthorize("hasAuthority('orden.reasignar')")
    @PostMapping("/{id}/reasignar")
    public ResponseEntity<OrdenResponse> reasignarOrden(@PathVariable UUID id,
                                                         @Valid @RequestBody ReasignarOrdenRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(ordenService.reasignarOrden(id, request));
    }
}
