package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import cfbd.co.sgt.dto.response.NotificacionResponse;
import cfbd.co.sgt.model.UsuarioEspecialidad;
import cfbd.co.sgt.repository.UsuarioEspecialidadRepository;
import cfbd.co.sgt.service.NotificacionPublisher;
import cfbd.co.sgt.service.OrdenNotificableEvent;

/**
 * Traduce los eventos de la cola de OT a avisos en tiempo real, solo tras el
 * commit (PRD E5). Resuelve los destinatarios según el tipo de aviso.
 */
@Component
public class NotificacionOrdenListener {

    private final UsuarioEspecialidadRepository usuarioEspecialidadRepository;
    private final NotificacionPublisher publisher;

    public NotificacionOrdenListener(UsuarioEspecialidadRepository usuarioEspecialidadRepository,
                                     NotificacionPublisher publisher) {
        this.usuarioEspecialidadRepository = usuarioEspecialidadRepository;
        this.publisher = publisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alConfirmar(OrdenNotificableEvent evento) {
        NotificacionResponse aviso = new NotificacionResponse(evento.tipo().evento(), evento.idOrden(),
                evento.numeroOrden(), evento.idEspecialidad());
        publisher.enviar(destinatarios(evento), aviso);
    }

    private List<UUID> destinatarios(OrdenNotificableEvent evento) {
        return switch (evento.tipo()) {
            case ORDEN_ASIGNADA -> List.of(evento.idUsuarioDestino());
            case ORDEN_ENCOLADA -> idsUsuario(usuarioEspecialidadRepository.findByEspecialidad(evento.idEspecialidad()), false);
            case ORDEN_DEVUELTA -> idsUsuario(usuarioEspecialidadRepository.findByEspecialidad(evento.idEspecialidad()), true);
            // El escalado lo publica EscaladoColaJob directamente, no por evento de dominio.
            case ORDEN_ESCALADA -> List.of();
        };
    }

    private List<UUID> idsUsuario(List<UsuarioEspecialidad> pertenencias, boolean soloResponsables) {
        return pertenencias.stream()
                .filter(pertenencia -> !soloResponsables || Boolean.TRUE.equals(pertenencia.getEs_responsable()))
                .map(pertenencia -> pertenencia.getUsuario().getId_usuario())
                .toList();
    }
}
