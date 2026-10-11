package cfbd.co.sgt.controller;

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

import cfbd.co.sgt.dto.request.EstadoNovedadRequest;
import cfbd.co.sgt.dto.request.WebNovedadRequest;
import cfbd.co.sgt.dto.response.PaginaResponse;
import cfbd.co.sgt.dto.response.WebNovedadResponse;
import cfbd.co.sgt.model.EstadoNovedad;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.WebNovedadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Panel de administración del sitio web: novedades (permisos web_novedad.*).
 * Publicar, programar o archivar exige además web_novedad.publish.
 */
@RestController
@RequestMapping("/api/web/novedades")
@RequiredArgsConstructor
public class WebNovedadController {

    private final WebNovedadService novedadService;
    private final FileStorageService fileStorageService;

    @PreAuthorize("hasAuthority('web_novedad.read')")
    @GetMapping
    public PaginaResponse<WebNovedadResponse> listar(@RequestParam(required = false) EstadoNovedad estado,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamano) {
        return novedadService.listar(estado, pagina, tamano);
    }

    @PreAuthorize("hasAuthority('web_novedad.read')")
    @GetMapping("/{id}")
    public WebNovedadResponse buscar(@PathVariable UUID id) {
        return novedadService.buscarPorId(id);
    }

    @PreAuthorize("hasAuthority('web_novedad.create')")
    @PostMapping
    public ResponseEntity<WebNovedadResponse> crear(@Valid @RequestBody WebNovedadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(novedadService.crear(request));
    }

    @PreAuthorize("hasAuthority('web_novedad.update')")
    @PutMapping("/{id}")
    public WebNovedadResponse editar(@PathVariable UUID id, @Valid @RequestBody WebNovedadRequest request) {
        return novedadService.editar(id, request);
    }

    @PreAuthorize("hasAuthority('web_novedad.publish')")
    @PatchMapping("/{id}/estado")
    public WebNovedadResponse cambiarEstado(@PathVariable UUID id, @Valid @RequestBody EstadoNovedadRequest request) {
        return novedadService.cambiarEstado(id, request);
    }

    @PreAuthorize("hasAuthority('web_novedad.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        novedadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('web_novedad.update')")
    @PostMapping("/{id}/imagen")
    public WebNovedadResponse subirImagen(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return novedadService.subirImagen(id, file);
    }

    @PreAuthorize("hasAuthority('web_novedad.update')")
    @DeleteMapping("/{id}/imagen")
    public WebNovedadResponse eliminarImagen(@PathVariable UUID id) {
        return novedadService.eliminarImagen(id);
    }

    // Vista previa en el panel (también de borradores y programadas).
    @PreAuthorize("hasAuthority('web_novedad.read')")
    @GetMapping("/{id}/imagen")
    public ResponseEntity<Resource> verImagen(@PathVariable UUID id) {
        return WebArchivoRespuesta.inline(fileStorageService, novedadService.obtenerReferenciaImagen(id),
                CacheControl.noStore());
    }
}
