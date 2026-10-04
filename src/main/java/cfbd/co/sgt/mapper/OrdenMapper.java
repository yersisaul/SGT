package cfbd.co.sgt.mapper;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Usuario;

/** Orden → OrdenResponse. Antes estaba copiado en tres servicios. */
@Component
public class OrdenMapper {

    public OrdenResponse toResponse(Orden orden) {
        OrdenResponse response = new OrdenResponse();
        response.setId_orden(orden.getId_orden());
        Usuario ejecutor = orden.getUsuario();
        response.setId_usuario(ejecutor != null ? ejecutor.getId_usuario() : null);
        response.setNombre_ejecutor(ejecutor != null
                ? (ejecutor.getNombres() + " " + ejecutor.getApellidos()).trim() : null);
        response.setId_especialidad(orden.getEspecialidad().getId_especialidad());
        response.setId_estado(orden.getEstado().getId_estado());
        response.setId_requerimiento(orden.getRequerimiento() != null ? orden.getRequerimiento().getId_requerimiento() : null);
        response.setId_solicitud(orden.getSolicitud() != null ? orden.getSolicitud().getId_solicitud() : null);
        response.setNumeroOrden(orden.getNumeroOrden());
        response.setFecha_registro(orden.getFecha_registro());
        response.setFecha_cierre(orden.getFecha_cierre());
        response.setUrl_adjunto(orden.getUrl_adjunto() != null
                ? "/api/archivos/ordenes/" + orden.getId_orden() : null);
        return response;
    }
}
