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
import cfbd.co.sgt.dto.request.WebCasoExitoRequest;
import cfbd.co.sgt.dto.response.WebCasoExitoResponse;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.WebCasoExitoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Panel de administración del sitio web: casos de éxito (permisos web_comercial.*). */
@RestController
@RequestMapping("/api/web/casos-exito")
@RequiredArgsConstructor
public class WebCasoExitoController {

    private final WebCasoExitoService casoService;
    private final FileStorageService fileStorageService;

    @PreAuthorize("hasAuthority('web_comercial.read')")
    @GetMapping
    public List<WebCasoExitoResponse> listar() {
        return casoService.listar();
    }

    @PreAuthorize("hasAuthority('web_comercial.read')")
    @GetMapping("/{id}")
    public WebCasoExitoResponse buscar(@PathVariable UUID id) {
        return casoService.buscarPorId(id);
    }

    @PreAuthorize("hasAuthority('web_comercial.create')")
    @PostMapping
    public ResponseEntity<WebCasoExitoResponse> crear(@Valid @RequestBody WebCasoExitoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(casoService.crear(request));
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @PutMapping("/{id}")
    public WebCasoExitoResponse editar(@PathVariable UUID id, @Valid @RequestBody WebCasoExitoRequest request) {
        return casoService.editar(id, request);
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @PatchMapping("/{id}/publicacion")
    public WebCasoExitoResponse cambiarPublicacion(@PathVariable UUID id,
            @Valid @RequestBody PublicacionWebRequest request) {
        return casoService.cambiarPublicacion(id, request.getPublicado());
    }

    @PreAuthorize("hasAuthority('web_comercial.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        casoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @PostMapping("/{id}/imagen")
    public WebCasoExitoResponse subirImagen(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return casoService.subirImagen(id, file);
    }

    @PreAuthorize("hasAuthority('web_comercial.update')")
    @DeleteMapping("/{id}/imagen")
    public WebCasoExitoResponse eliminarImagen(@PathVariable UUID id) {
        return casoService.eliminarImagen(id);
    }

    // Vista previa en el panel (también de casos no publicados).
    @PreAuthorize("hasAuthority('web_comercial.read')")
    @GetMapping("/{id}/imagen")
    public ResponseEntity<Resource> verImagen(@PathVariable UUID id) {
        return WebArchivoRespuesta.inline(fileStorageService, casoService.obtenerReferenciaImagen(id),
                CacheControl.noStore());
    }
}
