package cfbd.co.sgt.service;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.EstadoNovedadRequest;
import cfbd.co.sgt.dto.request.WebNovedadRequest;
import cfbd.co.sgt.dto.response.PaginaResponse;
import cfbd.co.sgt.dto.response.WebNovedadPublicoResponse;
import cfbd.co.sgt.dto.response.WebNovedadResponse;
import cfbd.co.sgt.model.EstadoNovedad;

public interface WebNovedadService {

    // ---- Panel de administración ----
    /** {@code estado} null = todas. */
    PaginaResponse<WebNovedadResponse> listar(EstadoNovedad estado, int pagina, int tamano);
    WebNovedadResponse buscarPorId(UUID id);
    /** Nace siempre en BORRADOR; el autor es el usuario autenticado. */
    WebNovedadResponse crear(WebNovedadRequest request);
    WebNovedadResponse editar(UUID id, WebNovedadRequest request);
    WebNovedadResponse cambiarEstado(UUID id, EstadoNovedadRequest request);
    void eliminar(UUID id);

    WebNovedadResponse subirImagen(UUID id, MultipartFile file);
    WebNovedadResponse eliminarImagen(UUID id);
    String obtenerReferenciaImagen(UUID id);

    // ---- Sitio web público: publicadas con fecha cumplida ----
    /** {@code categoria} = slug de la categoría, null = todas. Sin el contenido completo. */
    PaginaResponse<WebNovedadPublicoResponse> listarVisibles(String categoria, int pagina, int tamano);
    WebNovedadPublicoResponse buscarVisiblePorSlug(String slug);
    String obtenerReferenciaImagenPublica(UUID id);
}
