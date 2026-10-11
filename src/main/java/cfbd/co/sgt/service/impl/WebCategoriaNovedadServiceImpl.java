package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cfbd.co.sgt.dto.request.WebCategoriaNovedadRequest;
import cfbd.co.sgt.dto.response.WebCategoriaNovedadResponse;
import cfbd.co.sgt.dto.response.WebNovedadPublicoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.mapper.WebNovedadMapper;
import cfbd.co.sgt.model.WebCategoriaNovedad;
import cfbd.co.sgt.repository.WebCategoriaNovedadRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.WebCategoriaNovedadService;
import cfbd.co.sgt.service.WebTextos;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebCategoriaNovedadServiceImpl implements WebCategoriaNovedadService {

    private static final String RECURSO = "una categoría";

    private final WebCategoriaNovedadRepository categoriaRepository;
    private final WebNovedadMapper novedadMapper;
    private final UsuarioActualProvider usuarioActualProvider;

    @Override
    @Transactional(readOnly = true)
    public List<WebCategoriaNovedadResponse> listar() {
        return categoriaRepository.findAllOrdenadas().stream().map(novedadMapper::toCategoriaResponse).toList();
    }

    @Override
    @Transactional
    public WebCategoriaNovedadResponse crear(WebCategoriaNovedadRequest request) {
        WebCategoriaNovedad categoria = new WebCategoriaNovedad();
        categoria.setSlug(WebTextos.slugParaAlta(request.getSlug(), request.getNombre(),
                categoriaRepository::existsBySlug, RECURSO));
        categoria.setNombre(WebTextos.limpiar(request.getNombre()));
        categoria.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        return guardar(categoria);
    }

    @Override
    @Transactional
    public WebCategoriaNovedadResponse editar(UUID id, WebCategoriaNovedadRequest request) {
        WebCategoriaNovedad categoria = obtener(id);
        categoria.setSlug(WebTextos.slugParaEdicion(request.getSlug(), categoria.getSlug(),
                slug -> categoriaRepository.existsBySlugEnOtra(slug, id), RECURSO));
        categoria.setNombre(WebTextos.limpiar(request.getNombre()));
        if (request.getOrden() != null) {
            categoria.setOrden(request.getOrden());
        }
        return guardar(categoria);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        categoriaRepository.delete(obtener(id));
        // flush aquí: si una novedad la usa, el 409 sale dentro de esta operación.
        categoriaRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebNovedadPublicoResponse.CategoriaEnlace> listarPublicas() {
        return categoriaRepository.findAllOrdenadas().stream().map(novedadMapper::toCategoriaEnlace).toList();
    }

    private WebCategoriaNovedad obtener(UUID id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }

    private WebCategoriaNovedadResponse guardar(WebCategoriaNovedad categoria) {
        categoria.setUsuario_actualiza(usuarioActualProvider.obtener());
        return novedadMapper.toCategoriaResponse(categoriaRepository.saveAndFlush(categoria));
    }
}
