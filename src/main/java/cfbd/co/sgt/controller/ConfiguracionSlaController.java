package cfbd.co.sgt.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cfbd.co.sgt.dto.request.ConfiguracionSlaRequest;
import cfbd.co.sgt.dto.response.ConfiguracionSlaResponse;
import cfbd.co.sgt.service.ConfiguracionSlaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** SLA de despacho y atención editables desde el panel de Administración. */
@RestController
@RequestMapping("/api/configuracion/sla")
@RequiredArgsConstructor
public class ConfiguracionSlaController {

    private final ConfiguracionSlaService configuracionSlaService;

    @PreAuthorize("hasAuthority('sla.read')")
    @GetMapping
    public List<ConfiguracionSlaResponse> listar() {
        return configuracionSlaService.listar();
    }

    @PreAuthorize("hasAuthority('sla.update')")
    @PutMapping
    public List<ConfiguracionSlaResponse> actualizar(@Valid @RequestBody ConfiguracionSlaRequest request) {
        return configuracionSlaService.actualizar(request);
    }
}
