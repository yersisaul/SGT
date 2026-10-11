package cfbd.co.sgt.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.PublicacionWebRequest;
import cfbd.co.sgt.dto.request.WebPartnerRequest;
import cfbd.co.sgt.dto.response.WebPartnerResponse;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.WebPartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Panel de administración del sitio web: partners (permisos web_comercial.*). */
@RestController
@RequestMapping("/api/web/partners")
@RequiredArgsConstructor
public class WebPartnerController {

    private final WebPartnerService partnerService;
    private final FileStorageService fileStorageService;

    @PreAuthorize("hasAuthority('web_comercial.read')")
    @GetMapping
    public List<WebPartnerResponse> listar() {
        return partnerService.listar();
    }

    @PreAuthorize("hasAuthority('web_comercial.read')")
    @GetMapping("/{id}")
    public WebPartnerResponse buscar(@PathVariable UUID id) {
        return partnerService.buscarPorId(id);
    }

    @PreAuthorize("hasAuthority('web_comercial.create')")
    @PostMapping
    public ResponseEntity<WebPartnerResponse> crear(@Valid @RequestBody WebPartnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnerService.crear(request));
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @PutMapping("/{id}")
    public WebPartnerResponse editar(@PathVariable UUID id, @Valid @RequestBody WebPartnerRequest request) {
        return partnerService.editar(id, request);
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @PatchMapping("/{id}/publicacion")
    public WebPartnerResponse cambiarPublicacion(@PathVariable UUID id,
            @Valid @RequestBody PublicacionWebRequest request) {
        return partnerService.cambiarPublicacion(id, request.getPublicado());
    }

    @PreAuthorize("hasAuthority('web_comercial.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        partnerService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @PostMapping("/{id}/logo")
    public WebPartnerResponse subirLogo(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return partnerService.subirLogo(id, file);
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @DeleteMapping("/{id}/logo")
    public WebPartnerResponse eliminarLogo(@PathVariable UUID id) {
        return partnerService.eliminarLogo(id);
    }

    // Vista previa en el panel (también de partners no publicados).
    @PreAuthorize("hasAuthority('web_comercial.read')")
    @GetMapping("/{id}/logo")
    public ResponseEntity<Resource> verLogo(@PathVariable UUID id) {
        return WebArchivoRespuesta.inline(fileStorageService, partnerService.obtenerReferenciaLogo(id),
                CacheControl.noStore());
    }
}
