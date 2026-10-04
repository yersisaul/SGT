package cfbd.co.sgt.service;

import java.util.UUID;

/**
 * Evento de dominio publicado por la cola de OT dentro de la transacción. El
 * aviso SSE se envía solo tras el commit (NotificacionOrdenListener), para no
 * notificar algo que luego se revierte.
 *
 * @param idUsuarioDestino solo para ORDEN_ASIGNADA; null en los demás tipos.
 */
public record OrdenNotificableEvent(TipoNotificacion tipo, UUID idOrden, String numeroOrden,
                                    UUID idEspecialidad, UUID idUsuarioDestino) {
}
