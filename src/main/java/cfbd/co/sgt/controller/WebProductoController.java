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
import cfbd.co.sgt.dto.request.WebProductoRequest;
import cfbd.co.sgt.dto.response.WebProductoResponse;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.WebProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Panel de administración del sitio web: productos (permisos web_catalogo.*). */
@RestController
@RequestMapping("/api/web/productos")
@RequiredArgsConstructor
public class WebProductoController {

    private final WebProductoService productoService;
    private final FileStorageService fileStorageService;

    @PreAuthorize("hasAuthority('web_catalogo.read')")
    @GetMapping
    public List<WebProductoResponse> listar() {
        return productoService.listar();
    }

    @PreAuthorize("hasAuthority('web_catalogo.read')")
    @GetMapping("/{id}")
    public WebProductoResponse buscar(@PathVariable UUID id) {
        return productoService.buscarPorId(id);
    }

    @PreAuthorize("hasAuthority('web_catalogo.create')")
    @PostMapping
    public ResponseEntity<WebProductoResponse> crear(@Valid @RequestBody WebProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @PreAuthorize("hasAuthority('web_catalogo.update')")
    @PutMapping("/{id}")
    public WebProductoResponse editar(@PathVariable UUID id, @Valid @RequestBody WebProductoRequest request) {
        return productoService.editar(id, request);
    }

    @PreAuthorize("hasAuthority('web_catalogo.update')")
    @PatchMapping("/{id}/publicacion")
    public WebProductoResponse cambiarPublicacion(@PathVariable UUID id,
            @Valid @RequestBody PublicacionWebRequest request) {
        return productoService.cambiarPublicacion(id, request.getPublicado());
    }

    @PreAuthorize("hasAuthority('web_catalogo.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('web_catalogo.update')")
    @PostMapping("/{id}/imagen")
    public WebProductoResponse subirImagen(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return productoService.subirImagen(id, file);
    }

    @PreAuthorize("hasAuthority('web_catalogo.update')")
    @DeleteMapping("/{id}/imagen")
    public WebProductoResponse eliminarImagen(@PathVariable UUID id) {
        return productoService.eliminarImagen(id);
    }

    // Vista previa en el panel (también de productos no publicados).
    @PreAuthorize("hasAuthority('web_catalogo.read')")
    @GetMapping("/{id}/imagen")
    public ResponseEntity<Resource> verImagen(@PathVariable UUID id) {
        return WebArchivoRespuesta.inline(fileStorageService, productoService.obtenerReferenciaImagen(id),
                CacheControl.noStore());
    }
}
