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
import cfbd.co.sgt.dto.request.WebServicioRequest;
import cfbd.co.sgt.dto.response.WebServicioResponse;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.WebServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Panel de administración del sitio web: servicios (permisos web_catalogo.*). */
@RestController
@RequestMapping("/api/web/servicios")
@RequiredArgsConstructor
public class WebServicioController {

    private final WebServicioService servicioService;
    private final FileStorageService fileStorageService;

    @PreAuthorize("hasAuthority('web_catalogo.read')")
    @GetMapping
    public List<WebServicioResponse> listar() {
        return servicioService.listar();
    }

    @PreAuthorize("hasAuthority('web_catalogo.read')")
    @GetMapping("/{id}")
    public WebServicioResponse buscar(@PathVariable UUID id) {
        return servicioService.buscarPorId(id);
    }

    @PreAuthorize("hasAuthority('web_catalogo.create')")
    @PostMapping
    public ResponseEntity<WebServicioResponse> crear(@Valid @RequestBody WebServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.crear(request));
    }

    @PreAuthorize("hasAuthority('web_catalogo.update')")
    @PutMapping("/{id}")
    public WebServicioResponse editar(@PathVariable UUID id, @Valid @RequestBody WebServicioRequest request) {
        return servicioService.editar(id, request);
    }

    @PreAuthorize("hasAuthority('web_catalogo.update')")
    @PatchMapping("/{id}/publicacion")
    public WebServicioResponse cambiarPublicacion(@PathVariable UUID id,
            @Valid @RequestBody PublicacionWebRequest request) {
        return servicioService.cambiarPublicacion(id, request.getPublicado());
    }

    @PreAuthorize("hasAuthority('web_catalogo.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        servicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('web_catalogo.update')")
    @PostMapping("/{id}/imagen")
    public WebServicioResponse subirImagen(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return servicioService.subirImagen(id, file);
    }

    @PreAuthorize("hasAuthority('web_catalogo.update')")
    @DeleteMapping("/{id}/imagen")
    public WebServicioResponse eliminarImagen(@PathVariable UUID id) {
        return servicioService.eliminarImagen(id);
    }

    // Vista previa en el panel (también de servicios no publicados).
    @PreAuthorize("hasAuthority('web_catalogo.read')")
    @GetMapping("/{id}/imagen")
    public ResponseEntity<Resource> verImagen(@PathVariable UUID id) {
        return WebArchivoRespuesta.inline(fileStorageService, servicioService.obtenerReferenciaImagen(id),
                CacheControl.noStore());
    }
}
