package cfbd.co.sgt.controller;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cfbd.co.sgt.dto.response.WebCasoExitoPublicoResponse;
import cfbd.co.sgt.dto.response.PaginaResponse;
import cfbd.co.sgt.dto.response.WebClienteReferenciaPublicoResponse;
import cfbd.co.sgt.dto.response.WebDocumentoPublicoResponse;
import cfbd.co.sgt.dto.response.WebNovedadPublicoResponse;
import cfbd.co.sgt.dto.response.WebPartnerPublicoResponse;
import cfbd.co.sgt.dto.response.WebProductoPublicoResponse;
import cfbd.co.sgt.dto.response.WebServicioPublicoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.TipoDocumentoWeb;
import cfbd.co.sgt.model.TipoPartner;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.WebCasoExitoService;
import cfbd.co.sgt.service.WebCategoriaNovedadService;
import cfbd.co.sgt.service.WebClienteReferenciaService;
import cfbd.co.sgt.service.WebDocumentoService;
import cfbd.co.sgt.service.WebNovedadService;
import cfbd.co.sgt.service.WebPartnerService;
import cfbd.co.sgt.service.WebProductoService;
import cfbd.co.sgt.service.WebServicioService;
import lombok.RequiredArgsConstructor;

/**
 * API del sitio web público: solo GET y sin login (SecurityConfig), con su
 * propio rate limit. Cada service devuelve únicamente lo publicado; un
 * registro no publicado responde 404 igual que uno inexistente.
 */
@RestController
@RequestMapping("/api/public/web")
@RequiredArgsConstructor
public class WebPublicoController {

    // Contenido: un cambio del panel tarda como máximo esto en verse en el sitio.
    private static final CacheControl CACHE_CONTENIDO = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();
    // Imágenes: la URL cambia (?v=) cada vez que se reemplazan, así que pueden cachearse más.
    private static final CacheControl CACHE_ARCHIVOS = CacheControl.maxAge(Duration.ofDays(7)).cachePublic();

    private final WebProductoService productoService;
    private final WebServicioService servicioService;
    private final WebPartnerService partnerService;
    private final WebClienteReferenciaService clienteService;
    private final WebCasoExitoService casoService;
    private final WebNovedadService novedadService;
    private final WebCategoriaNovedadService categoriaNovedadService;
    private final WebDocumentoService documentoService;
    private final FileStorageService fileStorageService;

    @GetMapping("/productos")
    public ResponseEntity<List<WebProductoPublicoResponse>> listarProductos() {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO).body(productoService.listarPublicados());
    }

    @GetMapping("/productos/{slug}")
    public ResponseEntity<WebProductoPublicoResponse> buscarProducto(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO).body(productoService.buscarPublicadoPorSlug(slug));
    }

    @GetMapping("/servicios")
    public ResponseEntity<List<WebServicioPublicoResponse>> listarServicios() {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO).body(servicioService.listarPublicados());
    }

    @GetMapping("/servicios/{slug}")
    public ResponseEntity<WebServicioPublicoResponse> buscarServicio(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO).body(servicioService.buscarPublicadoPorSlug(slug));
    }

    /** {@code ?tipo=TECNOLOGICO | MARCA_COMPATIBLE} filtra; sin él, todos. */
    @GetMapping("/partners")
    public ResponseEntity<List<WebPartnerPublicoResponse>> listarPartners(
            @RequestParam(required = false) TipoPartner tipo) {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO).body(partnerService.listarPublicados(tipo));
    }

    @GetMapping("/clientes")
    public ResponseEntity<List<WebClienteReferenciaPublicoResponse>> listarClientes() {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO).body(clienteService.listarPublicados());
    }

    @GetMapping("/casos-exito")
    public ResponseEntity<List<WebCasoExitoPublicoResponse>> listarCasosExito() {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO).body(casoService.listarPublicados());
    }

    @GetMapping("/casos-exito/{slug}")
    public ResponseEntity<WebCasoExitoPublicoResponse> buscarCasoExito(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO).body(casoService.buscarPublicadoPorSlug(slug));
    }

    /**
     * Novedades publicadas cuya fecha ya llegó, destacadas primero y luego de
     * la más reciente a la más antigua. Sin el contenido completo (pedir el
     * detalle por slug). {@code tamano} entre 1 y 50.
     */
    @GetMapping("/novedades")
    public ResponseEntity<PaginaResponse<WebNovedadPublicoResponse>> listarNovedades(
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "10") int tamano) {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO)
                .body(novedadService.listarVisibles(categoria, pagina, tamano));
    }

    @GetMapping("/novedades/{slug}")
    public ResponseEntity<WebNovedadPublicoResponse> buscarNovedad(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO).body(novedadService.buscarVisiblePorSlug(slug));
    }

    @GetMapping("/categorias-novedad")
    public ResponseEntity<List<WebNovedadPublicoResponse.CategoriaEnlace>> listarCategoriasNovedad() {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO).body(categoriaNovedadService.listarPublicas());
    }

    /** Brochures y fichas publicados; filtros opcionales ?producto={slug}&tipo=BROCHURE|FICHA_TECNICA|PRESENTACION. */
    @GetMapping("/documentos")
    public ResponseEntity<List<WebDocumentoPublicoResponse>> listarDocumentos(
            @RequestParam(required = false) String producto, @RequestParam(required = false) TipoDocumentoWeb tipo) {
        return ResponseEntity.ok().cacheControl(CACHE_CONTENIDO).body(documentoService.listarPublicados(producto, tipo));
    }

    /**
     * Imagen, logo o PDF de un registro publicado;
     * {recurso} = productos | servicios | partners | clientes | casos-exito | novedades | documentos.
     */
    @GetMapping("/archivos/{recurso}/{id}")
    public ResponseEntity<Resource> verArchivo(@PathVariable String recurso, @PathVariable UUID id) {
        if (recurso.equals("documentos")) {
            WebDocumentoService.ArchivoDescarga pdf = documentoService.obtenerArchivoPublico(id);
            return WebArchivoRespuesta.pdf(fileStorageService, pdf.referencia(), pdf.nombre(), CACHE_ARCHIVOS);
        }
        String referencia = switch (recurso) {
            case "productos" -> productoService.obtenerReferenciaImagenPublica(id);
            case "servicios" -> servicioService.obtenerReferenciaImagenPublica(id);
            case "partners" -> partnerService.obtenerReferenciaLogoPublica(id);
            case "clientes" -> clienteService.obtenerReferenciaLogoPublica(id);
            case "casos-exito" -> casoService.obtenerReferenciaImagenPublica(id);
            case "novedades" -> novedadService.obtenerReferenciaImagenPublica(id);
            default -> throw new ResourceNotFoundException("Archivo no encontrado");
        };
        return WebArchivoRespuesta.inline(fileStorageService, referencia, CACHE_ARCHIVOS);
    }
}
