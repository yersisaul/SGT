package cfbd.co.sgt.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.MetricaCasoRequest;
import cfbd.co.sgt.dto.request.WebCasoExitoRequest;
import cfbd.co.sgt.dto.response.WebCasoExitoPublicoResponse;
import cfbd.co.sgt.dto.response.WebCasoExitoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.mapper.WebCasoExitoMapper;
import cfbd.co.sgt.model.MetricaCaso;
import cfbd.co.sgt.model.WebCasoExito;
import cfbd.co.sgt.repository.WebCasoExitoRepository;
import cfbd.co.sgt.repository.WebClienteReferenciaRepository;
import cfbd.co.sgt.repository.WebProductoRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import cfbd.co.sgt.service.WebCasoExitoService;
import cfbd.co.sgt.service.WebTextos;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebCasoExitoServiceImpl implements WebCasoExitoService {

    private static final String RECURSO = "un caso de éxito";
    private static final String NO_ENCONTRADO = "Caso de éxito no encontrado";

    private final WebCasoExitoRepository casoRepository;
    private final WebClienteReferenciaRepository clienteRepository;
    private final WebProductoRepository productoRepository;
    private final WebCasoExitoMapper casoMapper;
    private final FileStorageService fileStorageService;
    private final UsuarioActualProvider usuarioActualProvider;

    @Override
    @Transactional(readOnly = true)
    public List<WebCasoExitoResponse> listar() {
        return casoRepository.findAllOrdenados().stream().map(casoMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebCasoExitoResponse buscarPorId(UUID id) {
        return casoMapper.toResponse(obtener(id));
    }

    @Override
    @Transactional
    public WebCasoExitoResponse crear(WebCasoExitoRequest request) {
        WebCasoExito caso = new WebCasoExito();
        caso.setSlug(WebTextos.slugParaAlta(request.getSlug(), request.getTitulo(),
                casoRepository::existsBySlug, RECURSO));
        aplicar(caso, request);
        caso.setDestacado(Boolean.TRUE.equals(request.getDestacado()));
        caso.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        caso.setPublicado(Boolean.TRUE.equals(request.getPublicado()));
        return guardar(caso);
    }

    @Override
    @Transactional
    public WebCasoExitoResponse editar(UUID id, WebCasoExitoRequest request) {
        WebCasoExito caso = obtener(id);
        caso.setSlug(WebTextos.slugParaEdicion(request.getSlug(), caso.getSlug(),
                slug -> casoRepository.existsBySlugEnOtro(slug, id), RECURSO));
        aplicar(caso, request);
        if (request.getDestacado() != null) {
            caso.setDestacado(request.getDestacado());
        }
        if (request.getOrden() != null) {
            caso.setOrden(request.getOrden());
        }
        if (request.getPublicado() != null) {
            caso.setPublicado(request.getPublicado());
        }
        return guardar(caso);
    }

    @Override
    @Transactional
    public WebCasoExitoResponse cambiarPublicacion(UUID id, boolean publicado) {
        WebCasoExito caso = obtener(id);
        caso.setPublicado(publicado);
        return guardar(caso);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        WebCasoExito caso = obtener(id);
        String imagen = caso.getUrl_img();
        casoRepository.delete(caso);
        casoRepository.flush();
        fileStorageService.delete(imagen);
    }

    @Override
    @Transactional
    public WebCasoExitoResponse subirImagen(UUID id, MultipartFile file) {
        WebCasoExito caso = obtener(id);
        String referenciaAnterior = caso.getUrl_img();
        caso.setUrl_img(fileStorageService.store(file, TipoRecursoArchivo.WEB_CASOS_EXITO, id));
        WebCasoExitoResponse response = guardar(caso);
        fileStorageService.delete(referenciaAnterior);
        return response;
    }

    @Override
    @Transactional
    public WebCasoExitoResponse eliminarImagen(UUID id) {
        WebCasoExito caso = obtener(id);
        String referencia = caso.getUrl_img();
        if (referencia == null) {
            return casoMapper.toResponse(caso);
        }
        caso.setUrl_img(null);
        WebCasoExitoResponse response = guardar(caso);
        fileStorageService.delete(referencia);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerReferenciaImagen(UUID id) {
        return referenciaImagen(obtener(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebCasoExitoPublicoResponse> listarPublicados() {
        return casoRepository.findPublicados().stream().map(casoMapper::toPublicoResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebCasoExitoPublicoResponse buscarPublicadoPorSlug(String slug) {
        return casoRepository.findPublicadoPorSlug(slug)
                .map(casoMapper::toPublicoResponse)
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerReferenciaImagenPublica(UUID id) {
        // Un caso no publicado responde igual que uno inexistente.
        WebCasoExito caso = casoRepository.findById(id)
                .filter(WebCasoExito::isPublicado)
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
        return referenciaImagen(caso);
    }

    private WebCasoExito obtener(UUID id) {
        return casoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
    }

    private String referenciaImagen(WebCasoExito caso) {
        if (caso.getUrl_img() == null) {
            throw new ResourceNotFoundException("El caso de éxito no tiene imagen.");
        }
        return caso.getUrl_img();
    }

    private void aplicar(WebCasoExito caso, WebCasoExitoRequest request) {
        caso.setTitulo(WebTextos.limpiar(request.getTitulo()));
        caso.setResumen(WebTextos.limpiar(request.getResumen()));
        caso.setContenido(WebTextos.limpiar(request.getContenido()));
        caso.setUrl_externa(WebTextos.limpiar(request.getUrl_externa()));
        caso.setFecha_caso(request.getFecha_caso());
        caso.setMetricas(metricas(request.getMetricas()));
        // Vínculos opcionales: se reemplazan siempre (null = sin vínculo).
        caso.setCliente(request.getId_cliente_referencia() == null ? null
                : clienteRepository.findById(request.getId_cliente_referencia())
                        .orElseThrow(() -> new ResourceNotFoundException("Cliente de referencia no encontrado")));
        caso.setProducto(request.getId_producto() == null ? null
                : productoRepository.findById(request.getId_producto())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado")));
    }

    private List<MetricaCaso> metricas(List<MetricaCasoRequest> metricas) {
        List<MetricaCaso> resultado = new ArrayList<>();
        if (metricas != null) {
            metricas.forEach(m -> resultado.add(new MetricaCaso(WebTextos.limpiar(m.valor()),
                    WebTextos.limpiar(m.etiqueta()))));
        }
        return resultado;
    }

    // saveAndFlush: dispara @PreUpdate antes de mapear, así la respuesta trae
    // la fecha_actualizacion (y la versión de la URL de la imagen) nueva.
    private WebCasoExitoResponse guardar(WebCasoExito caso) {
        caso.setUsuario_actualiza(usuarioActualProvider.obtener());
        return casoMapper.toResponse(casoRepository.saveAndFlush(caso));
    }
}
