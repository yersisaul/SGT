package cfbd.co.sgt.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cfbd.co.sgt.dto.response.NotificacionResponse;
import cfbd.co.sgt.service.NotificacionPublisher;

/**
 * Conexiones SSE abiertas por usuario, en memoria (una sola instancia de la
 * aplicación — PRD §6). Limpia las conexiones cerradas y envía un heartbeat
 * periódico para que los proxies no corten el stream (NFR-008).
 */
@Component
public class SseEmitterRegistry implements NotificacionPublisher {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);
    private static final String COMENTARIO_HEARTBEAT = "ping";

    private final Map<UUID, Set<SseEmitter>> conexiones = new ConcurrentHashMap<>();
    private final long timeoutMillis;

    public SseEmitterRegistry(@Value("${app.sse.timeout-minutes:30}") long timeoutMinutos) {
        this.timeoutMillis = Duration.ofMinutes(timeoutMinutos).toMillis();
    }

    public SseEmitter registrar(UUID idUsuario) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        conexiones.computeIfAbsent(idUsuario, id -> new CopyOnWriteArraySet<>()).add(emitter);
        Runnable quitar = () -> quitar(idUsuario, emitter);
        emitter.onCompletion(quitar);
        emitter.onTimeout(quitar);
        emitter.onError(error -> quitar.run());
        return emitter;
    }

    @Override
    public void enviar(Collection<UUID> idsUsuario, NotificacionResponse notificacion) {
        for (UUID idUsuario : idsUsuario) {
            for (SseEmitter emitter : conexiones.getOrDefault(idUsuario, Set.of())) {
                enviarA(idUsuario, emitter, SseEmitter.event().name(notificacion.tipo()).data(notificacion));
            }
        }
    }

    @Scheduled(fixedRateString = "${app.sse.heartbeat-seconds:25}", timeUnit = java.util.concurrent.TimeUnit.SECONDS)
    public void heartbeat() {
        conexiones.forEach((idUsuario, emitters) -> emitters.forEach(
                emitter -> enviarA(idUsuario, emitter, SseEmitter.event().comment(COMENTARIO_HEARTBEAT))));
    }

    public int conexionesAbiertas() {
        return conexiones.values().stream().mapToInt(Set::size).sum();
    }

    private void enviarA(UUID idUsuario, SseEmitter emitter, SseEmitter.SseEventBuilder evento) {
        try {
            emitter.send(evento);
        } catch (IOException | IllegalStateException ex) {
            // Cliente desconectado: se descarta la conexión sin propagar el error.
            log.debug("SSE: conexión cerrada para el usuario {}, se descarta.", idUsuario);
            quitar(idUsuario, emitter);
        }
    }

    private void quitar(UUID idUsuario, SseEmitter emitter) {
        conexiones.computeIfPresent(idUsuario, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
