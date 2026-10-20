package cfbd.co.sgt.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import cfbd.co.sgt.service.CorreoService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class CorreoServiceImpl implements CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoServiceImpl.class);
    private static final String CARPETA_PLANTILLAS = "correo/";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final boolean habilitado;
    private final String remitente;
    private final String urlFrontend;

    public CorreoServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine,
                             @Value("${app.mail.enabled:false}") boolean habilitado,
                             @Value("${app.mail.from:no-reply@cfbd.co}") String remitente,
                             @Value("${app.mail.url-frontend:http://localhost:4200}") String urlFrontend) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.habilitado = habilitado;
        this.remitente = remitente;
        this.urlFrontend = urlFrontend.replaceAll("/+$", "");
    }

    @Async
    @Override
    public void enviar(Collection<String> destinatarios, String asunto, String plantilla, Map<String, Object> variables) {
        Set<String> unicos = new LinkedHashSet<>();
        destinatarios.stream().filter(correo -> correo != null && !correo.isBlank()).forEach(unicos::add);
        if (unicos.isEmpty()) {
            return;
        }
        if (!habilitado) {
            log.info("Correo deshabilitado (MAIL_ENABLED=false): '{}' a {} destinatario(s) no se envía.", asunto, unicos.size());
            return;
        }
        Context contexto = new Context();
        contexto.setVariables(variables);
        contexto.setVariable("urlFrontend", urlFrontend);
        String html = templateEngine.process(CARPETA_PLANTILLAS + plantilla, contexto);
        for (String destinatario : unicos) {
            try {
                MimeMessage mensaje = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, StandardCharsets.UTF_8.name());
                helper.setFrom(remitente);
                helper.setTo(destinatario);
                helper.setSubject(asunto);
                helper.setText(html, true);
                mailSender.send(mensaje);
            } catch (MessagingException | MailException e) {
                // Un fallo de SMTP no debe afectar al flujo: se registra y se sigue con el resto.
                log.warn("No se pudo enviar el correo '{}': {}", asunto, e.getMessage());
            }
        }
        log.info("Correo '{}' enviado a {} destinatario(s).", asunto, unicos.size());
    }
}
