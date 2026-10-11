package cfbd.co.sgt.service.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cfbd.co.sgt.dto.request.EstadoNovedadRequest;
import cfbd.co.sgt.dto.request.WebNovedadRequest;
import cfbd.co.sgt.dto.response.PaginaResponse;
import cfbd.co.sgt.dto.response.WebNovedadPublicoResponse;
import cfbd.co.sgt.dto.response.WebNovedadResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.mapper.WebNovedadMapper;
import cfbd.co.sgt.model.EstadoNovedad;
import cfbd.co.sgt.model.WebNovedad;
import cfbd.co.sgt.repository.WebCategoriaNovedadRepository;
import cfbd.co.sgt.repository.WebNovedadRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import cfbd.co.sgt.service.WebNovedadService;
import cfbd.co.sgt.service.WebTextos;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebNovedadServiceImpl implements WebNovedadService {

    private static final String RECURSO = "una novedad";
    private static final String NO_ENCONTRADA = "Novedad no encontrada";
    private static final int TAMANO_MAXIMO = 50;

    private final WebNovedadRepository novedadRepository;
    private final WebCategoriaNovedadRepository categoriaRepository;
    private final WebNovedadMapper novedadMapper;
    private final FileStorageService fileStorageService;
    private final UsuarioActualProvider usuarioActualProvider;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<WebNovedadResponse> listar(EstadoNovedad estado, int pagina, int tamano) {
        Pageable pageable = pagina(pagina, tamano);
        Page<WebNovedad> page = estado == null
                ? novedadRepository.findTodas(pageable)
                : novedadRepository.findPorEstado(estado, pageable);
        return PaginaResponse.de(page, novedadMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public WebNovedadResponse buscarPorId(UUID id) {
        return novedadMapper.toResponse(obtener(id));
    }

    @Override
    @Transactional
    public WebNovedadResponse crear(WebNovedadRequest request) {
        WebNovedad novedad = new WebNovedad();
        novedad.setSlug(WebTextos.slugParaAlta(request.getSlug(), request.getTitulo(),
                novedadRepository::existsBySlug, RECURSO));
        aplicar(novedad, request);
        novedad.setDestacado(Boolean.TRUE.equals(request.getDestacado()));
        novedad.setEstado(EstadoNovedad.BORRADOR);
        novedad.setAutor(usuarioActualProvider.obtener());
        return guardar(novedad);
    }

    @Override
    @Transactional
    public WebNovedadResponse editar(UUID id, WebNovedadRequest request) {
        WebNovedad novedad = obtener(id);
        novedad.setSlug(WebTextos.slugParaEdicion(request.getSlug(), novedad.getSlug(),
                slug -> novedadRepository.existsBySlugEnOtra(slug, id), RECURSO));
        aplicar(novedad, request);
        if (request.getDestacado() != null) {
            novedad.setDestacado(request.getDestacado());
        }
        if (novedad.getEstado() == EstadoNovedad.PUBLICADO && novedad.getContenido() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Una novedad publicada debe tener contenido; despublícala antes de vaciarlo.");
        }
        return guardar(novedad);
    }

    @Override
    @Transactional
    public WebNovedadResponse cambiarEstado(UUID id, EstadoNovedadRequest request) {
        WebNovedad novedad = obtener(id);
        if (request.getEstado() == EstadoNovedad.PUBLICADO) {
            if (novedad.getContenido() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La novedad necesita contenido para publicarse.");
            }
            // Fecha enviada (futura = programada) > la que ya tenía > ahora.
            if (request.getFecha_publicacion() != null) {
                novedad.setFecha_publicacion(request.getFecha_publicacion());
            } else if (novedad.getFecha_publicacion() == null) {
                novedad.setFecha_publicacion(Instant.now());
            }
        }
        novedad.setEstado(request.getEstado());
        return guardar(novedad);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        WebNovedad novedad = obtener(id);
        String portada = novedad.getUrl_portada();
        novedadRepository.delete(novedad);
        novedadRepository.flush();
        fileStorageService.delete(portada);
    }

    @Override
    @Transactional
    public WebNovedadResponse subirImagen(UUID id, MultipartFile file) {
        WebNovedad novedad = obtener(id);
        String referenciaAnterior = novedad.getUrl_portada();
        novedad.setUrl_portada(fileStorageService.store(file, TipoRecursoArchivo.WEB_NOVEDADES, id));
        WebNovedadResponse response = guardar(novedad);
        fileStorageService.delete(referenciaAnterior);
        return response;
    }

    @Override
    @Transactional
    public WebNovedadResponse eliminarImagen(UUID id) {
        WebNovedad novedad = obtener(id);
        String referencia = novedad.getUrl_portada();
        if (referencia == null) {
            return novedadMapper.toResponse(novedad);
        }
        novedad.setUrl_portada(null);
        WebNovedadResponse response = guardar(novedad);
        fileStorageService.delete(referencia);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerReferenciaImagen(UUID id) {
        return referenciaPortada(obtener(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<WebNovedadPublicoResponse> listarVisibles(String categoria, int pagina, int tamano) {
        Pageable pageable = pagina(pagina, tamano);
        Instant ahora = Instant.now();
        String slugCategoria = WebTextos.limpiar(categoria);
        Page<WebNovedad> page = slugCategoria == null
                ? novedadRepository.findVisibles(EstadoNovedad.PUBLICADO, ahora, pageable)
                : novedadRepository.findVisiblesPorCategoria(EstadoNovedad.PUBLICADO, ahora, slugCategoria, pageable);
        return PaginaResponse.de(page, n -> novedadMapper.toPublicoResponse(n, false));
    }

    @Override
    @Transactional(readOnly = true)
    public WebNovedadPublicoResponse buscarVisiblePorSlug(String slug) {
        // Borrador, archivada o programada responden igual que una inexistente.
        return novedadRepository.findBySlug(slug)
                .filter(n -> n.esVisibleEn(Instant.now()))
                .map(n -> novedadMapper.toPublicoResponse(n, true))
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADA));
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerReferenciaImagenPublica(UUID id) {
        WebNovedad novedad = novedadRepository.findById(id)
                .filter(n -> n.esVisibleEn(Instant.now()))
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADA));
        return referenciaPortada(novedad);
    }

    private WebNovedad obtener(UUID id) {
        return novedadRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADA));
    }

    private String referenciaPortada(WebNovedad novedad) {
        if (novedad.getUrl_portada() == null) {
            throw new ResourceNotFoundException("La novedad no tiene portada.");
        }
        return novedad.getUrl_portada();
    }

    private void aplicar(WebNovedad novedad, WebNovedadRequest request) {
        novedad.setTitulo(WebTextos.limpiar(request.getTitulo()));
        novedad.setResumen(WebTextos.limpiar(request.getResumen()));
        novedad.setContenido(WebTextos.limpiar(request.getContenido()));
        // Se reemplaza siempre (null = sin categoría).
        novedad.setCategoria(request.getId_categoria_novedad() == null ? null
                : categoriaRepository.findById(request.getId_categoria_novedad())
                        .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada")));
    }

    /** Página desde 0; tamaño entre 1 y 50 (valores fuera de rango se ajustan). */
    private Pageable pagina(int pagina, int tamano) {
        return PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), TAMANO_MAXIMO));
    }

    // saveAndFlush: dispara @PreUpdate antes de mapear, así la respuesta trae
    // la fecha_actualizacion (y la versión de la URL de la portada) nueva.
    private WebNovedadResponse guardar(WebNovedad novedad) {
        novedad.setUsuario_actualiza(usuarioActualProvider.obtener());
        return novedadMapper.toResponse(novedadRepository.saveAndFlush(novedad));
    }
}
