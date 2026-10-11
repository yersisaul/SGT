package cfbd.co.sgt.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.WebProductoRequest;
import cfbd.co.sgt.dto.response.WebProductoPublicoResponse;
import cfbd.co.sgt.dto.response.WebProductoResponse;

public interface WebProductoService {

    // ---- Panel de administración ----
    List<WebProductoResponse> listar();
    WebProductoResponse buscarPorId(UUID id);
    WebProductoResponse crear(WebProductoRequest request);
    WebProductoResponse editar(UUID id, WebProductoRequest request);
    WebProductoResponse cambiarPublicacion(UUID id, boolean publicado);
    void eliminar(UUID id);

    WebProductoResponse subirImagen(UUID id, MultipartFile file);
    WebProductoResponse eliminarImagen(UUID id);
    String obtenerReferenciaImagen(UUID id);

    // ---- Sitio web público: solo publicados ----
    List<WebProductoPublicoResponse> listarPublicados();
    WebProductoPublicoResponse buscarPublicadoPorSlug(String slug);
    String obtenerReferenciaImagenPublica(UUID id);
}
