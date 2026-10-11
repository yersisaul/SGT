package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.WebServicioRequest;
import cfbd.co.sgt.dto.response.WebServicioPublicoResponse;
import cfbd.co.sgt.dto.response.WebServicioResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.mapper.WebServicioMapper;
import cfbd.co.sgt.model.WebServicio;
import cfbd.co.sgt.repository.WebServicioRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import cfbd.co.sgt.service.WebServicioService;
import cfbd.co.sgt.service.WebTextos;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebServicioServiceImpl implements WebServicioService {

    private static final String RECURSO = "un servicio";
    private static final String NO_ENCONTRADO = "Servicio no encontrado";

    private final WebServicioRepository servicioRepository;
    private final WebServicioMapper servicioMapper;
    private final FileStorageService fileStorageService;
    private final UsuarioActualProvider usuarioActualProvider;

    @Override
    @Transactional(readOnly = true)
    public List<WebServicioResponse> listar() {
        return servicioRepository.findAllOrdenados().stream().map(servicioMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebServicioResponse buscarPorId(UUID id) {
        return servicioMapper.toResponse(obtener(id));
    }

    @Override
    @Transactional
    public WebServicioResponse crear(WebServicioRequest request) {
        WebServicio servicio = new WebServicio();
        servicio.setSlug(WebTextos.slugParaAlta(request.getSlug(), request.getNombre(),
                servicioRepository::existsBySlug, RECURSO));
        aplicar(servicio, request);
        servicio.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        servicio.setPublicado(Boolean.TRUE.equals(request.getPublicado()));
        return guardar(servicio);
    }

    @Override
    @Transactional
    public WebServicioResponse editar(UUID id, WebServicioRequest request) {
        WebServicio servicio = obtener(id);
        servicio.setSlug(WebTextos.slugParaEdicion(request.getSlug(), servicio.getSlug(),
                slug -> servicioRepository.existsBySlugEnOtro(slug, id), RECURSO));
        aplicar(servicio, request);
        if (request.getOrden() != null) {
            servicio.setOrden(request.getOrden());
        }
        if (request.getPublicado() != null) {
            servicio.setPublicado(request.getPublicado());
        }
        return guardar(servicio);
    }

    @Override
    @Transactional
    public WebServicioResponse cambiarPublicacion(UUID id, boolean publicado) {
        WebServicio servicio = obtener(id);
        servicio.setPublicado(publicado);
        return guardar(servicio);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        WebServicio servicio = obtener(id);
        String imagen = servicio.getUrl_img();
        servicioRepository.delete(servicio);
        // flush aquí: si otro registro lo referencia, el 409 sale antes de borrar la imagen.
        servicioRepository.flush();
        fileStorageService.delete(imagen);
    }

    @Override
    @Transactional
    public WebServicioResponse subirImagen(UUID id, MultipartFile file) {
        WebServicio servicio = obtener(id);
        String referenciaAnterior = servicio.getUrl_img();
        servicio.setUrl_img(fileStorageService.store(file, TipoRecursoArchivo.WEB_SERVICIOS, id));
        WebServicioResponse response = guardar(servicio);
        fileStorageService.delete(referenciaAnterior);
        return response;
    }

    @Override
    @Transactional
    public WebServicioResponse eliminarImagen(UUID id) {
        WebServicio servicio = obtener(id);
        String referencia = servicio.getUrl_img();
        if (referencia == null) {
            return servicioMapper.toResponse(servicio);
        }
        servicio.setUrl_img(null);
        WebServicioResponse response = guardar(servicio);
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
    public List<WebServicioPublicoResponse> listarPublicados() {
        return servicioRepository.findPublicados().stream().map(servicioMapper::toPublicoResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebServicioPublicoResponse buscarPublicadoPorSlug(String slug) {
        return servicioRepository.findPublicadoPorSlug(slug)
                .map(servicioMapper::toPublicoResponse)
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerReferenciaImagenPublica(UUID id) {
        // Un servicio no publicado responde igual que uno inexistente.
        WebServicio servicio = servicioRepository.findById(id)
                .filter(WebServicio::isPublicado)
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
        return referenciaImagen(servicio);
    }

    private WebServicio obtener(UUID id) {
        return servicioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
    }

    private String referenciaImagen(WebServicio servicio) {
        if (servicio.getUrl_img() == null) {
            throw new ResourceNotFoundException("El servicio no tiene imagen.");
        }
        return servicio.getUrl_img();
    }

    private void aplicar(WebServicio servicio, WebServicioRequest request) {
        servicio.setNombre(WebTextos.limpiar(request.getNombre()));
        servicio.setDescripcion_corta(WebTextos.limpiar(request.getDescripcion_corta()));
        servicio.setDescripcion(WebTextos.limpiar(request.getDescripcion()));
        servicio.setIcono(WebTextos.limpiar(request.getIcono()));
        servicio.setCaracteristicas(WebTextos.limpiarLista(request.getCaracteristicas()));
    }

    // saveAndFlush: dispara @PreUpdate antes de mapear, así la respuesta trae
    // la fecha_actualizacion (y la versión de la URL de la imagen) nueva.
    private WebServicioResponse guardar(WebServicio servicio) {
        servicio.setUsuario_actualiza(usuarioActualProvider.obtener());
        return servicioMapper.toResponse(servicioRepository.saveAndFlush(servicio));
    }
}
