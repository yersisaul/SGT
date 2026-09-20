package cfbd.co.sgt.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Cuerpo (opcional) de POST /solicitudes/{id}/generar-orden y
 * POST /requerimientos/{id}/generar-orden. Todo lo demás (especialidad,
 * usuario, estado inicial, número) se deriva en el Service a partir de la
 * Solicitud/Requerimiento de origen y del usuario autenticado, nunca del
 * cliente.
 */
@Getter
@Setter
public class GenerarOrdenRequest {
    private String comentario;
    private String url_adjunto;
}
