package cfbd.co.sgt.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.WebPartnerRequest;
import cfbd.co.sgt.dto.response.WebPartnerPublicoResponse;
import cfbd.co.sgt.dto.response.WebPartnerResponse;
import cfbd.co.sgt.model.TipoPartner;

public interface WebPartnerService {

    // ---- Panel de administración ----
    List<WebPartnerResponse> listar();
    WebPartnerResponse buscarPorId(UUID id);
    WebPartnerResponse crear(WebPartnerRequest request);
    WebPartnerResponse editar(UUID id, WebPartnerRequest request);
    WebPartnerResponse cambiarPublicacion(UUID id, boolean publicado);
    void eliminar(UUID id);

    WebPartnerResponse subirLogo(UUID id, MultipartFile file);
    WebPartnerResponse eliminarLogo(UUID id);
    String obtenerReferenciaLogo(UUID id);

    // ---- Sitio web público: solo publicados ----
    /** {@code tipo} null = todos. */
    List<WebPartnerPublicoResponse> listarPublicados(TipoPartner tipo);
    String obtenerReferenciaLogoPublica(UUID id);
}
