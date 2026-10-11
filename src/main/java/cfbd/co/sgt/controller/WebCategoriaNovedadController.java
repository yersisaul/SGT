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

import cfbd.co.sgt.dto.request.WebCategoriaNovedadRequest;
import cfbd.co.sgt.dto.response.WebCategoriaNovedadResponse;
import cfbd.co.sgt.service.WebCategoriaNovedadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Panel de administración del sitio web: categorías de novedades (permisos web_novedad.*). */
@RestController
@RequestMapping("/api/web/categorias-novedad")
@RequiredArgsConstructor
public class WebCategoriaNovedadController {

    private final WebCategoriaNovedadService categoriaService;

    @PreAuthorize("hasAuthority('web_novedad.read')")
    @GetMapping
    public List<WebCategoriaNovedadResponse> listar() {
        return categoriaService.listar();
    }

    @PreAuthorize("hasAuthority('web_novedad.create')")
    @PostMapping
    public ResponseEntity<WebCategoriaNovedadResponse> crear(@Valid @RequestBody WebCategoriaNovedadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
    }

    @PreAuthorize("hasAuthority('web_novedad.update')")
    @PutMapping("/{id}")
    public WebCategoriaNovedadResponse editar(@PathVariable UUID id,
            @Valid @RequestBody WebCategoriaNovedadRequest request) {
        return categoriaService.editar(id, request);
    }

    @PreAuthorize("hasAuthority('web_novedad.delete')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
