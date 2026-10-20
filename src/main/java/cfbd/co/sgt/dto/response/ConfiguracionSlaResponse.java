package cfbd.co.sgt.dto.response;

import java.time.Instant;

/** Un SLA editable con su valor vigente y el inicial del .env (para "restablecer"). */
public record ConfiguracionSlaResponse(String clave, String grupo, String etiqueta, int horas,
                                       int horas_por_defecto, Instant fecha_modificacion,
                                       String modificado_por) {
}
