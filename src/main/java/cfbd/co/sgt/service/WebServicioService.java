package cfbd.co.sgt.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.WebServicioRequest;
import cfbd.co.sgt.dto.response.WebServicioPublicoResponse;
import cfbd.co.sgt.dto.response.WebServicioResponse;

public interface WebServicioService {

    // ---- Panel de administración ----
    List<WebServicioResponse> listar();
    WebServicioResponse buscarPorId(UUID id);
    WebServicioResponse crear(WebServicioRequest request);
    WebServicioResponse editar(UUID id, WebServicioRequest request);
    WebServicioResponse cambiarPublicacion(UUID id, boolean publicado);
    void eliminar(UUID id);

    WebServicioResponse subirImagen(UUID id, MultipartFile file);
    WebServicioResponse eliminarImagen(UUID id);
    String obtenerReferenciaImagen(UUID id);

    // ---- Sitio web público: solo publicados ----
    List<WebServicioPublicoResponse> listarPublicados();
    WebServicioPublicoResponse buscarPublicadoPorSlug(String slug);
    String obtenerReferenciaImagenPublica(UUID id);
}
