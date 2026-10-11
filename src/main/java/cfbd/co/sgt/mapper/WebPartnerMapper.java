package cfbd.co.sgt.mapper;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.WebPartnerPublicoResponse;
import cfbd.co.sgt.dto.response.WebPartnerResponse;
import cfbd.co.sgt.model.WebPartner;

@Component
public class WebPartnerMapper {

    public WebPartnerResponse toResponse(WebPartner partner) {
        WebPartnerResponse response = new WebPartnerResponse();
        response.setId_partner(partner.getId_partner());
        response.setNombre(partner.getNombre());
        response.setCategoria(partner.getCategoria());
        response.setDescripcion(partner.getDescripcion());
        response.setUrl_logo(partner.getUrl_logo() != null
                ? WebMapperSoporte.archivoAdmin("partners", partner.getId_partner(), "logo",
                        partner.getFecha_actualizacion())
                : null);
        response.setUrl_sitio(partner.getUrl_sitio());
        response.setTipo(partner.getTipo());
        response.setOrden(partner.getOrden());
        response.setPublicado(partner.isPublicado());
        response.setFecha_creacion(partner.getFecha_creacion());
        response.setFecha_actualizacion(partner.getFecha_actualizacion());
        response.setNombre_usuario_actualiza(WebMapperSoporte.nombreCompleto(partner.getUsuario_actualiza()));
        return response;
    }

    public WebPartnerPublicoResponse toPublicoResponse(WebPartner partner) {
        WebPartnerPublicoResponse response = new WebPartnerPublicoResponse();
        response.setNombre(partner.getNombre());
        response.setCategoria(partner.getCategoria());
        response.setDescripcion(partner.getDescripcion());
        response.setUrl_logo(partner.getUrl_logo() != null
                ? WebMapperSoporte.imagenPublica("partners", partner.getId_partner(), partner.getFecha_actualizacion())
                : null);
        response.setUrl_sitio(partner.getUrl_sitio());
        response.setTipo(partner.getTipo());
        return response;
    }
}
