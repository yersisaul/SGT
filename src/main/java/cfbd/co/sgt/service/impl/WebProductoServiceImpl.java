package cfbd.co.sgt.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.ModuloProductoRequest;
import cfbd.co.sgt.dto.request.WebProductoRequest;
import cfbd.co.sgt.dto.response.WebProductoPublicoResponse;
import cfbd.co.sgt.dto.response.WebProductoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.mapper.WebProductoMapper;
import cfbd.co.sgt.model.ModuloProducto;
import cfbd.co.sgt.model.WebProducto;
import cfbd.co.sgt.repository.WebProductoRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import cfbd.co.sgt.service.WebProductoService;
import cfbd.co.sgt.service.WebTextos;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebProductoServiceImpl implements WebProductoService {

    private static final String RECURSO = "un producto";
    private static final String NO_ENCONTRADO = "Producto no encontrado";

    private final WebProductoRepository productoRepository;
    private final WebProductoMapper productoMapper;
    private final FileStorageService fileStorageService;
    private final UsuarioActualProvider usuarioActualProvider;

    @Override
    @Transactional(readOnly = true)
    public List<WebProductoResponse> listar() {
        return productoRepository.findAllOrdenados().stream().map(productoMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebProductoResponse buscarPorId(UUID id) {
        return productoMapper.toResponse(obtener(id));
    }

    @Override
    @Transactional
    public WebProductoResponse crear(WebProductoRequest request) {
        WebProducto producto = new WebProducto();
        producto.setSlug(WebTextos.slugParaAlta(request.getSlug(), request.getNombre(),
                productoRepository::existsBySlug, RECURSO));
        aplicar(producto, request);
        producto.setDestacado(Boolean.TRUE.equals(request.getDestacado()));
        producto.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        producto.setPublicado(Boolean.TRUE.equals(request.getPublicado()));
        return guardar(producto);
    }

    @Override
    @Transactional
    public WebProductoResponse editar(UUID id, WebProductoRequest request) {
        WebProducto producto = obtener(id);
        producto.setSlug(WebTextos.slugParaEdicion(request.getSlug(), producto.getSlug(),
                slug -> productoRepository.existsBySlugEnOtro(slug, id), RECURSO));
        aplicar(producto, request);
        if (request.getDestacado() != null) {
            producto.setDestacado(request.getDestacado());
        }
        if (request.getOrden() != null) {
            producto.setOrden(request.getOrden());
        }
        if (request.getPublicado() != null) {
            producto.setPublicado(request.getPublicado());
        }
        return guardar(producto);
    }

    @Override
    @Transactional
    public WebProductoResponse cambiarPublicacion(UUID id, boolean publicado) {
        WebProducto producto = obtener(id);
        producto.setPublicado(publicado);
        return guardar(producto);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        WebProducto producto = obtener(id);
        String imagen = producto.getUrl_img();
        productoRepository.delete(producto);
        // flush aquí: si otro registro lo referencia, el 409 sale antes de borrar la imagen.
        productoRepository.flush();
        fileStorageService.delete(imagen);
    }

    @Override
    @Transactional
    public WebProductoResponse subirImagen(UUID id, MultipartFile file) {
        WebProducto producto = obtener(id);
        String referenciaAnterior = producto.getUrl_img();
        producto.setUrl_img(fileStorageService.store(file, TipoRecursoArchivo.WEB_PRODUCTOS, id));
        WebProductoResponse response = guardar(producto);
        fileStorageService.delete(referenciaAnterior);
        return response;
    }

    @Override
    @Transactional
    public WebProductoResponse eliminarImagen(UUID id) {
        WebProducto producto = obtener(id);
        String referencia = producto.getUrl_img();
        if (referencia == null) {
            return productoMapper.toResponse(producto);
        }
        producto.setUrl_img(null);
        WebProductoResponse response = guardar(producto);
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
    public List<WebProductoPublicoResponse> listarPublicados() {
        return productoRepository.findPublicados().stream().map(productoMapper::toPublicoResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebProductoPublicoResponse buscarPublicadoPorSlug(String slug) {
        return productoRepository.findPublicadoPorSlug(slug)
                .map(productoMapper::toPublicoResponse)
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerReferenciaImagenPublica(UUID id) {
        // Un producto no publicado responde igual que uno inexistente.
        WebProducto producto = productoRepository.findById(id)
                .filter(WebProducto::isPublicado)
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
        return referenciaImagen(producto);
    }

    private WebProducto obtener(UUID id) {
        return productoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
    }

    private String referenciaImagen(WebProducto producto) {
        if (producto.getUrl_img() == null) {
            throw new ResourceNotFoundException("El producto no tiene imagen.");
        }
        return producto.getUrl_img();
    }

    private void aplicar(WebProducto producto, WebProductoRequest request) {
        producto.setNombre(WebTextos.limpiar(request.getNombre()));
        producto.setSubtitulo(WebTextos.limpiar(request.getSubtitulo()));
        producto.setDescripcion_corta(WebTextos.limpiar(request.getDescripcion_corta()));
        producto.setDescripcion(WebTextos.limpiar(request.getDescripcion()));
        producto.setUrl_video(WebTextos.limpiar(request.getUrl_video()));
        producto.setCaracteristicas(WebTextos.limpiarLista(request.getCaracteristicas()));
        producto.setModulos(modulos(request.getModulos()));
    }

    private List<ModuloProducto> modulos(List<ModuloProductoRequest> modulos) {
        List<ModuloProducto> resultado = new ArrayList<>();
        if (modulos != null) {
            modulos.forEach(m -> resultado.add(new ModuloProducto(WebTextos.limpiar(m.nombre()),
                    WebTextos.limpiar(m.grupo()), WebTextos.limpiar(m.descripcion()))));
        }
        return resultado;
    }

    // saveAndFlush: dispara @PreUpdate antes de mapear, así la respuesta trae
    // la fecha_actualizacion (y la versión de la URL de la imagen) nueva.
    private WebProductoResponse guardar(WebProducto producto) {
        producto.setUsuario_actualiza(usuarioActualProvider.obtener());
        return productoMapper.toResponse(productoRepository.saveAndFlush(producto));
    }
}
