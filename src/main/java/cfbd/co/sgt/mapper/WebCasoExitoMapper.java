package cfbd.co.sgt.mapper;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.WebCasoExitoPublicoResponse;
import cfbd.co.sgt.dto.response.WebCasoExitoResponse;
import cfbd.co.sgt.model.WebCasoExito;
import cfbd.co.sgt.model.WebClienteReferencia;
import cfbd.co.sgt.model.WebProducto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebCasoExitoMapper {

    private final WebClienteReferenciaMapper clienteMapper;

    public WebCasoExitoResponse toResponse(WebCasoExito caso) {
        WebCasoExitoResponse response = new WebCasoExitoResponse();
        response.setId_caso_exito(caso.getId_caso_exito());
        response.setSlug(caso.getSlug());
        response.setTitulo(caso.getTitulo());
        response.setResumen(caso.getResumen());
        response.setContenido(caso.getContenido());
        response.setUrl_img(caso.getUrl_img() != null
                ? WebMapperSoporte.imagenAdmin("casos-exito", caso.getId_caso_exito(), caso.getFecha_actualizacion())
                : null);
        response.setUrl_externa(caso.getUrl_externa());
        response.setFecha_caso(caso.getFecha_caso());
        response.setMetricas(WebMapperSoporte.listaOVacia(caso.getMetricas()));
        WebClienteReferencia cliente = caso.getCliente();
        if (cliente != null) {
            response.setId_cliente_referencia(cliente.getId_cliente_referencia());
            response.setNombre_cliente(cliente.getNombre());
        }
        WebProducto producto = caso.getProducto();
        if (producto != null) {
            response.setId_producto(producto.getId_producto());
            response.setNombre_producto(producto.getNombre());
        }
        response.setDestacado(caso.isDestacado());
        response.setOrden(caso.getOrden());
        response.setPublicado(caso.isPublicado());
        response.setFecha_creacion(caso.getFecha_creacion());
        response.setFecha_actualizacion(caso.getFecha_actualizacion());
        response.setNombre_usuario_actualiza(WebMapperSoporte.nombreCompleto(caso.getUsuario_actualiza()));
        return response;
    }

    public WebCasoExitoPublicoResponse toPublicoResponse(WebCasoExito caso) {
        WebCasoExitoPublicoResponse response = new WebCasoExitoPublicoResponse();
        response.setSlug(caso.getSlug());
        response.setTitulo(caso.getTitulo());
        response.setResumen(caso.getResumen());
        response.setContenido(caso.getContenido());
        response.setUrl_img(caso.getUrl_img() != null
                ? WebMapperSoporte.imagenPublica("casos-exito", caso.getId_caso_exito(), caso.getFecha_actualizacion())
                : null);
        response.setUrl_externa(caso.getUrl_externa());
        response.setFecha_caso(caso.getFecha_caso());
        response.setMetricas(WebMapperSoporte.listaOVacia(caso.getMetricas()));
        // Un cliente o producto despublicado no se filtra al sitio a través del caso.
        WebClienteReferencia cliente = caso.getCliente();
        if (cliente != null && cliente.isPublicado()) {
            response.setCliente(clienteMapper.toPublicoResponse(cliente));
        }
        WebProducto producto = caso.getProducto();
        if (producto != null && producto.isPublicado()) {
            response.setProducto(new WebCasoExitoPublicoResponse.ProductoEnlace(producto.getSlug(), producto.getNombre()));
        }
        response.setDestacado(caso.isDestacado());
        return response;
    }
}
