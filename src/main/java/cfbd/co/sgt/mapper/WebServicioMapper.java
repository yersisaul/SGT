package cfbd.co.sgt.mapper;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.WebServicioPublicoResponse;
import cfbd.co.sgt.dto.response.WebServicioResponse;
import cfbd.co.sgt.model.WebServicio;

@Component
public class WebServicioMapper {

    public WebServicioResponse toResponse(WebServicio servicio) {
        WebServicioResponse response = new WebServicioResponse();
        response.setId_servicio(servicio.getId_servicio());
        response.setSlug(servicio.getSlug());
        response.setNombre(servicio.getNombre());
        response.setDescripcion_corta(servicio.getDescripcion_corta());
        response.setDescripcion(servicio.getDescripcion());
        response.setIcono(servicio.getIcono());
        response.setUrl_img(servicio.getUrl_img() != null
                ? WebMapperSoporte.imagenAdmin("servicios", servicio.getId_servicio(), servicio.getFecha_actualizacion())
                : null);
        response.setCaracteristicas(WebMapperSoporte.listaOVacia(servicio.getCaracteristicas()));
        response.setOrden(servicio.getOrden());
        response.setPublicado(servicio.isPublicado());
        response.setFecha_creacion(servicio.getFecha_creacion());
        response.setFecha_actualizacion(servicio.getFecha_actualizacion());
        response.setNombre_usuario_actualiza(WebMapperSoporte.nombreCompleto(servicio.getUsuario_actualiza()));
        return response;
    }

    public WebServicioPublicoResponse toPublicoResponse(WebServicio servicio) {
        WebServicioPublicoResponse response = new WebServicioPublicoResponse();
        response.setSlug(servicio.getSlug());
        response.setNombre(servicio.getNombre());
        response.setDescripcion_corta(servicio.getDescripcion_corta());
        response.setDescripcion(servicio.getDescripcion());
        response.setIcono(servicio.getIcono());
        response.setUrl_img(servicio.getUrl_img() != null
                ? WebMapperSoporte.imagenPublica("servicios", servicio.getId_servicio(), servicio.getFecha_actualizacion())
                : null);
        response.setCaracteristicas(WebMapperSoporte.listaOVacia(servicio.getCaracteristicas()));
        return response;
    }
}
