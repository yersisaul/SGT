package cfbd.co.sgt.mapper;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.WebClienteReferenciaPublicoResponse;
import cfbd.co.sgt.dto.response.WebClienteReferenciaResponse;
import cfbd.co.sgt.model.WebClienteReferencia;

@Component
public class WebClienteReferenciaMapper {

    public WebClienteReferenciaResponse toResponse(WebClienteReferencia cliente) {
        WebClienteReferenciaResponse response = new WebClienteReferenciaResponse();
        response.setId_cliente_referencia(cliente.getId_cliente_referencia());
        response.setNombre(cliente.getNombre());
        response.setSector(cliente.getSector());
        response.setUrl_logo(cliente.getUrl_logo() != null
                ? WebMapperSoporte.archivoAdmin("clientes", cliente.getId_cliente_referencia(), "logo",
                        cliente.getFecha_actualizacion())
                : null);
        response.setUrl_sitio(cliente.getUrl_sitio());
        response.setOrden(cliente.getOrden());
        response.setPublicado(cliente.isPublicado());
        response.setFecha_creacion(cliente.getFecha_creacion());
        response.setFecha_actualizacion(cliente.getFecha_actualizacion());
        response.setNombre_usuario_actualiza(WebMapperSoporte.nombreCompleto(cliente.getUsuario_actualiza()));
        return response;
    }

    public WebClienteReferenciaPublicoResponse toPublicoResponse(WebClienteReferencia cliente) {
        WebClienteReferenciaPublicoResponse response = new WebClienteReferenciaPublicoResponse();
        response.setNombre(cliente.getNombre());
        response.setSector(cliente.getSector());
        response.setUrl_logo(cliente.getUrl_logo() != null
                ? WebMapperSoporte.imagenPublica("clientes", cliente.getId_cliente_referencia(),
                        cliente.getFecha_actualizacion())
                : null);
        response.setUrl_sitio(cliente.getUrl_sitio());
        return response;
    }
}
