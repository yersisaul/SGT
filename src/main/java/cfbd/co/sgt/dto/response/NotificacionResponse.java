package cfbd.co.sgt.dto.response;

import java.util.UUID;

/**
 * Payload de un aviso SSE. Sin datos personales: el cliente pide el detalle
 * a la API con sus propios permisos (PRD §6).
 */
public record NotificacionResponse(String tipo, UUID id_orden, String numero_orden, UUID id_especialidad) {
}
