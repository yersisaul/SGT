package cfbd.co.sgt.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cfbd.co.sgt.dto.request.EquipoEspecialidadRequest;
import cfbd.co.sgt.dto.response.MiEspecialidadResponse;
import cfbd.co.sgt.dto.response.MiembroEspecialidadResponse;
import cfbd.co.sgt.service.EquipoEspecialidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Equipos por especialidad (PRD E2): miembros y responsables. */
@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EquipoEspecialidadController {

    private final EquipoEspecialidadService equipoEspecialidadService;

    // Especialidades del usuario autenticado (para su cola de OT).
    @PreAuthorize("hasAuthority('especialidad.read')")
    @GetMapping("/mias")
    public List<MiEspecialidadResponse> getMisEspecialidades() {
        return equipoEspecialidadService.listarMisEspecialidades();
    }

    // El responsable necesita ver su equipo para asignar OT (orden.asignar).
    @PreAuthorize("hasAnyAuthority('especialidad.gestionar_equipo','orden.asignar')")
    @GetMapping("/{id}/miembros")
    public List<MiembroEspecialidadResponse> getMiembros(@PathVariable UUID id) {
        return equipoEspecialidadService.listarMiembros(id);
    }

    @PreAuthorize("hasAuthority('especialidad.gestionar_equipo')")
    @PutMapping("/{id}/miembros")
    public List<MiembroEspecialidadResponse> putMiembros(@PathVariable UUID id,
                                                         @Valid @RequestBody EquipoEspecialidadRequest request) {
        return equipoEspecialidadService.reemplazarMiembros(id, request);
    }
}
