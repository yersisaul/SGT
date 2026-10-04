package cfbd.co.sgt.service;

import java.util.Collection;
import java.util.UUID;

import cfbd.co.sgt.dto.response.NotificacionResponse;

/** Envía avisos a usuarios conectados. La implementación SSE es reemplazable (p. ej. por un broker). */
public interface NotificacionPublisher {

    void enviar(Collection<UUID> idsUsuario, NotificacionResponse notificacion);
}
