package cfbd.co.sgt.mapper;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.WebProductoPublicoResponse;
import cfbd.co.sgt.dto.response.WebProductoResponse;
import cfbd.co.sgt.model.WebProducto;

@Component
public class WebProductoMapper {

    public WebProductoResponse toResponse(WebProducto producto) {
        WebProductoResponse response = new WebProductoResponse();
        response.setId_producto(producto.getId_producto());
        response.setSlug(producto.getSlug());
        response.setNombre(producto.getNombre());
        response.setSubtitulo(producto.getSubtitulo());
        response.setDescripcion_corta(producto.getDescripcion_corta());
        response.setDescripcion(producto.getDescripcion());
        response.setUrl_img(producto.getUrl_img() != null
                ? WebMapperSoporte.imagenAdmin("productos", producto.getId_producto(), producto.getFecha_actualizacion())
                : null);
        response.setUrl_video(producto.getUrl_video());
        response.setCaracteristicas(WebMapperSoporte.listaOVacia(producto.getCaracteristicas()));
        response.setModulos(WebMapperSoporte.listaOVacia(producto.getModulos()));
        response.setDestacado(producto.isDestacado());
        response.setOrden(producto.getOrden());
        response.setPublicado(producto.isPublicado());
        response.setFecha_creacion(producto.getFecha_creacion());
        response.setFecha_actualizacion(producto.getFecha_actualizacion());
        response.setNombre_usuario_actualiza(WebMapperSoporte.nombreCompleto(producto.getUsuario_actualiza()));
        return response;
    }

    public WebProductoPublicoResponse toPublicoResponse(WebProducto producto) {
        WebProductoPublicoResponse response = new WebProductoPublicoResponse();
        response.setSlug(producto.getSlug());
        response.setNombre(producto.getNombre());
        response.setSubtitulo(producto.getSubtitulo());
        response.setDescripcion_corta(producto.getDescripcion_corta());
        response.setDescripcion(producto.getDescripcion());
        response.setUrl_img(producto.getUrl_img() != null
                ? WebMapperSoporte.imagenPublica("productos", producto.getId_producto(), producto.getFecha_actualizacion())
                : null);
        response.setUrl_video(producto.getUrl_video());
        response.setCaracteristicas(WebMapperSoporte.listaOVacia(producto.getCaracteristicas()));
        response.setModulos(WebMapperSoporte.listaOVacia(producto.getModulos()));
        response.setDestacado(producto.isDestacado());
        return response;
    }
}
