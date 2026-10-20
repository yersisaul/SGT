package cfbd.co.sgt.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.RequerimientoRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.service.CorreoService;
import cfbd.co.sgt.service.EstadosNegocio;
import cfbd.co.sgt.service.SlaCalculator;

/**
 * Avisa por correo a los Administradores (usuarios con requerimiento.aprobar)
 * cuando un Requerimiento sigue esperando decisión ("En revisión") después de
 * su SLA (observación 2026-10-19). Un solo correo por revisión con todos los
 * RQ recién vencidos; cada RQ se avisa una vez (requerimiento.fecha_aviso_sla).
 */
@Component
public class AvisoSlaRequerimientoJob {

    private static final Logger log = LoggerFactory.getLogger(AvisoSlaRequerimientoJob.class);
    private static final String PERMISO_DECIDE = "requerimiento.aprobar";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RequerimientoRepository requerimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SlaCalculator slaCalculator;
    private final CorreoService correoService;
    private final ZoneId zona;

    public AvisoSlaRequerimientoJob(RequerimientoRepository requerimientoRepository, UsuarioRepository usuarioRepository,
                                    SlaCalculator slaCalculator, CorreoService correoService,
                                    @Value("${app.kpi.zona-horaria:America/Lima}") String zonaHoraria) {
        this.requerimientoRepository = requerimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.slaCalculator = slaCalculator;
        this.correoService = correoService;
        this.zona = ZoneId.of(zonaHoraria);
    }

    @Scheduled(initialDelayString = "${app.mail.aviso-sla-revision-segundos:300}",
            fixedDelayString = "${app.mail.aviso-sla-revision-segundos:300}", timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void revisar() {
        Instant ahora = Instant.now();
        List<Requerimiento> vencidos = new ArrayList<>(requerimientoRepository
                .findSinAvisoSlaEnEstado(EstadosNegocio.EN_REVISION).stream()
                .filter(requerimiento -> requerimiento.getFecha_registro() != null
                        && slaCalculator.deadlineRequerimiento(requerimiento.getFecha_registro()).isBefore(ahora))
                .toList());
        if (vencidos.isEmpty()) {
            return;
        }
        vencidos.sort(Comparator.comparing(Requerimiento::getFecha_registro));
        List<String> correos = usuarioRepository.findByPermiso(PERMISO_DECIDE).stream()
                .filter(Usuario::isHabilitado).map(Usuario::getEmail).toList();
        List<Map<String, Object>> filas = vencidos.stream().map(requerimiento -> fila(requerimiento, ahora)).toList();
        String asunto = vencidos.size() == 1
                ? "Requerimiento " + vencidos.get(0).getNumeroRequerimiento() + " superó el SLA de decisión"
                : vencidos.size() + " requerimientos superaron el SLA de decisión";
        correoService.enviar(correos, asunto, "requerimiento-sla-vencido", Map.of("requerimientos", filas));
        // Se marca aunque no haya destinatarios: el aviso es informativo y no se reintenta.
        vencidos.forEach(requerimiento -> requerimiento.setFecha_aviso_sla(ahora));
        log.info("Aviso de SLA vencido: {} RQ, {} Administrador(es).", vencidos.size(), correos.size());
    }

    private Map<String, Object> fila(Requerimiento requerimiento, Instant ahora) {
        Instant limite = slaCalculator.deadlineRequerimiento(requerimiento.getFecha_registro());
        long horasVencido = Math.max(0, Duration.between(limite, ahora).toHours());
        return Map.of(
                "numero", requerimiento.getNumeroRequerimiento(),
                "id", requerimiento.getId_requerimiento(),
                "especialidad", requerimiento.getEspecialidad().getNombre(),
                "descripcion", requerimiento.getDescripcion() == null ? "" : requerimiento.getDescripcion(),
                "registro", FORMATO_FECHA.format(requerimiento.getFecha_registro().atZone(zona)),
                "limite", FORMATO_FECHA.format(limite.atZone(zona)),
                "vencidoHace", horasVencido < 1 ? "menos de 1 h" : horasVencido + " h");
    }
}
