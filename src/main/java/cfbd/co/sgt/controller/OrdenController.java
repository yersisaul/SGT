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
import cfbd.co.sgt.dto.request.AsignarOrdenRequest;
import cfbd.co.sgt.dto.request.ReasignarOrdenRequest;
import cfbd.co.sgt.dto.request.VerificarOrdenRequest;
import cfbd.co.sgt.dto.response.AsignacionOrdenResponse;
import cfbd.co.sgt.dto.response.CargaMiembroResponse;
import cfbd.co.sgt.dto.response.OrdenEscaladaResponse;
import cfbd.co.sgt.service.escalado.EscaladoColaService;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.service.ColaOrdenService;
import cfbd.co.sgt.service.OrdenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
public class OrdenController {

    private final OrdenService ordenService;
    private final ColaOrdenService colaOrdenService;
    private final EscaladoColaService escaladoColaService;

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

    // ---- Cola de OT por especialidad (CLAUDE.md §1 pasos 10-12) ----
    // El permiso habilita la operación; la regla por recurso (miembro,
    // responsable, ejecutor) la valida ColaOrdenService.

    @PreAuthorize("hasAuthority('orden.read')")
    @GetMapping("/cola")
    public List<OrdenResponse> getCola() {
        return colaOrdenService.listarCola();
    }

    @PreAuthorize("hasAnyAuthority('orden.asignar','orden.read_all')")
    @GetMapping("/equipo/{idEspecialidad}")
    public List<CargaMiembroResponse> getCargaEquipo(@PathVariable UUID idEspecialidad) {
        return colaOrdenService.listarCargaEquipo(idEspecialidad);
    }

    // OT que superaron el umbral de espera en cola (escalado por prioridad).
    @PreAuthorize("hasAnyAuthority('orden.asignar','orden.read_all')")
    @GetMapping("/escaladas")
    public List<OrdenEscaladaResponse> getEscaladas() {
        return escaladoColaService.listarVisibles();
    }

    @PreAuthorize("hasAuthority('orden.read')")
    @GetMapping("/{id}/asignaciones")
    public List<AsignacionOrdenResponse> getAsignaciones(@PathVariable UUID id) {
        return colaOrdenService.listarAsignaciones(id);
    }

    @PreAuthorize("hasAuthority('orden.tomar')")
    @PostMapping("/{id}/tomar")
    public OrdenResponse tomarOrden(@PathVariable UUID id) {
        return colaOrdenService.tomar(id);
    }

    @PreAuthorize("hasAuthority('orden.asignar')")
    @PostMapping("/{id}/asignar")
    public OrdenResponse asignarOrden(@PathVariable UUID id, @Valid @RequestBody AsignarOrdenRequest request) {
        return colaOrdenService.asignar(id, request);
    }

    @PreAuthorize("hasAuthority('orden.verificar')")
    @PostMapping("/{id}/verificar")
    public OrdenResponse verificarOrden(@PathVariable UUID id, @Valid @RequestBody VerificarOrdenRequest request) {
        return colaOrdenService.verificar(id, request);
    }

    // Reasignar a otra especialidad (paso 12): responsable de la especialidad
    // actual o Administrador (PRD D14), validado en el Service.
    @PreAuthorize("hasAuthority('orden.reasignar')")
    @PostMapping("/{id}/reasignar")
    public OrdenResponse reasignarOrden(@PathVariable UUID id, @Valid @RequestBody ReasignarOrdenRequest request) {
        return colaOrdenService.reasignar(id, request);
    }
}
