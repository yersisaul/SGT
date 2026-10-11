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
import cfbd.co.sgt.dto.request.WebClienteReferenciaRequest;
import cfbd.co.sgt.dto.response.WebClienteReferenciaResponse;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.WebClienteReferenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Panel de administración del sitio web: clientes de referencia (permisos web_comercial.*). */
@RestController
@RequestMapping("/api/web/clientes")
@RequiredArgsConstructor
public class WebClienteReferenciaController {

    private final WebClienteReferenciaService clienteService;
    private final FileStorageService fileStorageService;

    @PreAuthorize("hasAuthority('web_comercial.read')")
    @GetMapping
    public List<WebClienteReferenciaResponse> listar() {
        return clienteService.listar();
    }

    @PreAuthorize("hasAuthority('web_comercial.read')")
    @GetMapping("/{id}")
    public WebClienteReferenciaResponse buscar(@PathVariable UUID id) {
        return clienteService.buscarPorId(id);
    }

    @PreAuthorize("hasAuthority('web_comercial.create')")
    @PostMapping
    public ResponseEntity<WebClienteReferenciaResponse> crear(@Valid @RequestBody WebClienteReferenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @PutMapping("/{id}")
    public WebClienteReferenciaResponse editar(@PathVariable UUID id,
            @Valid @RequestBody WebClienteReferenciaRequest request) {
        return clienteService.editar(id, request);
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @PatchMapping("/{id}/publicacion")
    public WebClienteReferenciaResponse cambiarPublicacion(@PathVariable UUID id,
            @Valid @RequestBody PublicacionWebRequest request) {
        return clienteService.cambiarPublicacion(id, request.getPublicado());
    }

    @PreAuthorize("hasAuthority('web_comercial.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @PostMapping("/{id}/logo")
    public WebClienteReferenciaResponse subirLogo(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return clienteService.subirLogo(id, file);
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @DeleteMapping("/{id}/logo")
    public WebClienteReferenciaResponse eliminarLogo(@PathVariable UUID id) {
        return clienteService.eliminarLogo(id);
    }

    // Vista previa en el panel (también de clientes no publicados).
    @PreAuthorize("hasAuthority('web_comercial.read')")
    @GetMapping("/{id}/logo")
    public ResponseEntity<Resource> verLogo(@PathVariable UUID id) {
        return WebArchivoRespuesta.inline(fileStorageService, clienteService.obtenerReferenciaLogo(id),
                CacheControl.noStore());
    }
}
