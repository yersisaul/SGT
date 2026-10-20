package cfbd.co.sgt.service;

import java.util.Collection;
import java.util.Map;

/** Envío de correos informativos (observación 2026-10-19). Asíncrono: nunca bloquea ni revierte la operación de negocio. */
public interface CorreoService {

    /**
     * Envía un correo HTML a cada destinatario por separado (nadie ve los
     * correos de los demás).
     *
     * @param plantilla nombre de la plantilla Thymeleaf en templates/correo/
     * @param variables datos que usa la plantilla
     */
    void enviar(Collection<String> destinatarios, String asunto, String plantilla, Map<String, Object> variables);
}
