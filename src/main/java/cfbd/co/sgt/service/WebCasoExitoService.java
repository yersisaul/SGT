package cfbd.co.sgt.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.WebCasoExitoRequest;
import cfbd.co.sgt.dto.response.WebCasoExitoPublicoResponse;
import cfbd.co.sgt.dto.response.WebCasoExitoResponse;

public interface WebCasoExitoService {

    // ---- Panel de administración ----
    List<WebCasoExitoResponse> listar();
    WebCasoExitoResponse buscarPorId(UUID id);
    WebCasoExitoResponse crear(WebCasoExitoRequest request);
    WebCasoExitoResponse editar(UUID id, WebCasoExitoRequest request);
    WebCasoExitoResponse cambiarPublicacion(UUID id, boolean publicado);
    void eliminar(UUID id);

    WebCasoExitoResponse subirImagen(UUID id, MultipartFile file);
    WebCasoExitoResponse eliminarImagen(UUID id);
    String obtenerReferenciaImagen(UUID id);

    // ---- Sitio web público: solo publicados ----
    List<WebCasoExitoPublicoResponse> listarPublicados();
    WebCasoExitoPublicoResponse buscarPublicadoPorSlug(String slug);
    String obtenerReferenciaImagenPublica(UUID id);
}
