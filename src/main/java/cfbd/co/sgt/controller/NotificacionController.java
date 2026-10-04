package cfbd.co.sgt.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cfbd.co.sgt.config.SseEmitterRegistry;
import cfbd.co.sgt.security.UsuarioActualProvider;
import lombok.RequiredArgsConstructor;

/**
 * Stream de avisos en tiempo real (PRD E5). El JWT llega en el header
 * Authorization (el frontend usa fetch, no EventSource), nunca en la URL.
 * Solo se envían avisos dirigidos al propio usuario.
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final SseEmitterRegistry registry;
    private final UsuarioActualProvider usuarioActual;

    @PreAuthorize("hasAuthority('orden.read')")
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return registry.registrar(usuarioActual.obtener().getId_usuario());
    }
}
