package cfbd.co.sgt.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.WebClienteReferenciaRequest;
import cfbd.co.sgt.dto.response.WebClienteReferenciaPublicoResponse;
import cfbd.co.sgt.dto.response.WebClienteReferenciaResponse;

public interface WebClienteReferenciaService {

    // ---- Panel de administración ----
    List<WebClienteReferenciaResponse> listar();
    WebClienteReferenciaResponse buscarPorId(UUID id);
    WebClienteReferenciaResponse crear(WebClienteReferenciaRequest request);
    WebClienteReferenciaResponse editar(UUID id, WebClienteReferenciaRequest request);
    WebClienteReferenciaResponse cambiarPublicacion(UUID id, boolean publicado);
    /** 409 si algún caso de éxito lo referencia. */
    void eliminar(UUID id);

    WebClienteReferenciaResponse subirLogo(UUID id, MultipartFile file);
    WebClienteReferenciaResponse eliminarLogo(UUID id);
    String obtenerReferenciaLogo(UUID id);

    // ---- Sitio web público: solo publicados ----
    List<WebClienteReferenciaPublicoResponse> listarPublicados();
    String obtenerReferenciaLogoPublica(UUID id);
}
