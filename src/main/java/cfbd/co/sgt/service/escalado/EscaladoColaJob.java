package cfbd.co.sgt.service.escalado;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cfbd.co.sgt.dto.response.NotificacionResponse;
import cfbd.co.sgt.dto.response.OrdenEscaladaResponse;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.UsuarioEspecialidadRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.service.NotificacionPublisher;
import cfbd.co.sgt.service.TipoNotificacion;
import lombok.RequiredArgsConstructor;

/**
 * Revisa periódicamente la cola y avisa en tiempo real: nivel 1 a los
 * responsables de la especialidad, nivel 2 a los Administradores (usuarios
 * con orden.read_all y orden.reasignar). Cada (OT, nivel, entrada a la cola)
 * se avisa una sola vez; el registro vive en memoria, así que tras un
 * reinicio una OT ya escalada se vuelve a avisar una vez.
 */
@Component
@RequiredArgsConstructor
class EscaladoColaJob {

    private static final Logger log = LoggerFactory.getLogger(EscaladoColaJob.class);
    private static final String PERMISO_ALCANCE_GLOBAL = "orden.read_all";
    private static final String PERMISO_REASIGNAR = "orden.reasignar";

    private final EscaladoColaService escaladoColaService;
    private final UsuarioEspecialidadRepository usuarioEspecialidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionPublisher publisher;

    private final Set<String> avisados = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelayString = "${app.escalado.revision-segundos:60}", timeUnit = java.util.concurrent.TimeUnit.SECONDS)
    @Transactional(readOnly = true)
    public void revisar() {
        List<OrdenEscaladaResponse> escaladas = escaladoColaService.detectar();
        Set<String> vigentes = escaladas.stream().map(this::clave).collect(Collectors.toSet());
        // Las OT que ya se tomaron o reasignaron dejan de figurar.
        avisados.retainAll(vigentes);
        for (OrdenEscaladaResponse escalada : escaladas) {
            if (avisados.add(clave(escalada))) {
                publisher.enviar(destinatarios(escalada), new NotificacionResponse(TipoNotificacion.ORDEN_ESCALADA.evento(),
                        escalada.id_orden(), escalada.numero_orden(), escalada.id_especialidad()));
                log.info("Escalado nivel {}: {} lleva {} min en cola de {}.", escalada.nivel(), escalada.numero_orden(),
                        escalada.minutos_en_cola(), escalada.especialidad());
            }
        }
    }

    private String clave(OrdenEscaladaResponse escalada) {
        return escalada.id_orden() + "|" + escalada.nivel() + "|" + escalada.en_cola_desde();
    }

    private List<UUID> destinatarios(OrdenEscaladaResponse escalada) {
        if (escalada.nivel() >= UmbralesEscalado.NIVEL_ADMINISTRADOR) {
            Set<UUID> conReasignar = usuarioRepository.findByPermiso(PERMISO_REASIGNAR).stream()
                    .map(Usuario::getId_usuario).collect(Collectors.toSet());
            return usuarioRepository.findByPermiso(PERMISO_ALCANCE_GLOBAL).stream()
                    .map(Usuario::getId_usuario).filter(conReasignar::contains).toList();
        }
        return usuarioEspecialidadRepository.findByEspecialidad(escalada.id_especialidad()).stream()
                .filter(pertenencia -> Boolean.TRUE.equals(pertenencia.getEs_responsable()))
                .map(pertenencia -> pertenencia.getUsuario().getId_usuario())
                .toList();
    }
}
