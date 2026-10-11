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
import cfbd.co.sgt.dto.request.WebDocumentoRequest;
import cfbd.co.sgt.dto.response.WebDocumentoResponse;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.WebDocumentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Panel de administración del sitio web: brochures y fichas técnicas (permisos web_documento.*). */
@RestController
@RequestMapping("/api/web/documentos")
@RequiredArgsConstructor
public class WebDocumentoController {

    private final WebDocumentoService documentoService;
    private final FileStorageService fileStorageService;

    @PreAuthorize("hasAuthority('web_documento.read')")
    @GetMapping
    public List<WebDocumentoResponse> listar() {
        return documentoService.listar();
    }

    @PreAuthorize("hasAuthority('web_documento.read')")
    @GetMapping("/{id}")
    public WebDocumentoResponse buscar(@PathVariable UUID id) {
        return documentoService.buscarPorId(id);
    }

    @PreAuthorize("hasAuthority('web_documento.create')")
    @PostMapping
    public ResponseEntity<WebDocumentoResponse> crear(@Valid @RequestBody WebDocumentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentoService.crear(request));
    }

    @PreAuthorize("hasAuthority('web_documento.update')")
    @PutMapping("/{id}")
    public WebDocumentoResponse editar(@PathVariable UUID id, @Valid @RequestBody WebDocumentoRequest request) {
        return documentoService.editar(id, request);
    }

    @PreAuthorize("hasAuthority('web_documento.update')")
    @PatchMapping("/{id}/publicacion")
    public WebDocumentoResponse cambiarPublicacion(@PathVariable UUID id,
            @Valid @RequestBody PublicacionWebRequest request) {
        return documentoService.cambiarPublicacion(id, request.getPublicado());
    }

    @PreAuthorize("hasAuthority('web_documento.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        documentoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // Solo PDF, hasta WEB_DOCUMENTO_MAX_SIZE_MB. Reemplaza el anterior.
    @PreAuthorize("hasAuthority('web_documento.update')")
    @PostMapping("/{id}/archivo")
    public WebDocumentoResponse subirArchivo(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return documentoService.subirArchivo(id, file);
    }

    // Vista previa en el panel (también de documentos no publicados).
    @PreAuthorize("hasAuthority('web_documento.read')")
    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> verArchivo(@PathVariable UUID id) {
        WebDocumentoService.ArchivoDescarga archivo = documentoService.obtenerArchivo(id);
        return WebArchivoRespuesta.pdf(fileStorageService, archivo.referencia(), archivo.nombre(),
                CacheControl.noStore());
    }
}
