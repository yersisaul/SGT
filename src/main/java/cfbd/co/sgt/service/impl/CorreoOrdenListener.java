package cfbd.co.sgt.service.impl;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.model.UsuarioEspecialidad;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.repository.UsuarioEspecialidadRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.service.CorreoService;
import cfbd.co.sgt.service.OrdenNotificableEvent;
import cfbd.co.sgt.service.TipoNotificacion;

/**
 * Correo informativo al trabajador de Operaciones que recibe una OT
 * (observación 2026-10-19): a los miembros de la especialidad cuando la OT
 * entra a su cola, y al ejecutor cuando un responsable se la asigna. Igual
 * que el aviso SSE, solo tras el commit.
 */
@Component
public class CorreoOrdenListener {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrdenRepository ordenRepository;
    private final UsuarioEspecialidadRepository usuarioEspecialidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorreoService correoService;
    private final ZoneId zona;

    public CorreoOrdenListener(OrdenRepository ordenRepository,
                               UsuarioEspecialidadRepository usuarioEspecialidadRepository,
                               UsuarioRepository usuarioRepository, CorreoService correoService,
                               @Value("${app.kpi.zona-horaria:America/Lima}") String zonaHoraria) {
        this.ordenRepository = ordenRepository;
        this.usuarioEspecialidadRepository = usuarioEspecialidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.correoService = correoService;
        this.zona = ZoneId.of(zonaHoraria);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alConfirmar(OrdenNotificableEvent evento) {
        String motivo = switch (evento.tipo()) {
            case ORDEN_ENCOLADA -> "Ingresó a la cola de tu especialidad";
            case ORDEN_ASIGNADA -> "Te fue asignada";
            default -> null;
        };
        if (motivo == null) {
            return;
        }
        List<Usuario> destinatarios = destinatarios(evento);
        if (destinatarios.isEmpty()) {
            return;
        }
        ordenRepository.findById(evento.idOrden()).ifPresent(orden -> correoService.enviar(
                destinatarios.stream().map(Usuario::getEmail).toList(),
                "Orden de trabajo " + orden.getNumeroOrden() + " · " + motivo.toLowerCase(),
                "orden-recibida", variables(orden, motivo)));
    }

    private List<Usuario> destinatarios(OrdenNotificableEvent evento) {
        List<Usuario> usuarios = evento.tipo() == TipoNotificacion.ORDEN_ASIGNADA
                ? usuarioRepository.findById(evento.idUsuarioDestino()).stream().toList()
                : usuarioEspecialidadRepository.findByEspecialidad(evento.idEspecialidad()).stream()
                        .map(UsuarioEspecialidad::getUsuario).toList();
        return usuarios.stream().filter(Usuario::isHabilitado).toList();
    }

    private Map<String, Object> variables(Orden orden, String motivo) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("motivo", motivo);
        variables.put("numeroOrden", orden.getNumeroOrden());
        variables.put("idOrden", orden.getId_orden());
        variables.put("especialidad", orden.getEspecialidad().getNombre());
        variables.put("fechaRegistro", orden.getFecha_registro() == null ? "" : FORMATO_FECHA.format(orden.getFecha_registro().atZone(zona)));
        if (orden.getSolicitud() != null) {
            variables.put("origen", "Solicitud " + orden.getSolicitud().getNumeroSolicitud());
            variables.put("prioridad", orden.getSolicitud().getPrioridad());
            variables.put("descripcion", orden.getSolicitud().getDescripcion());
            variables.put("activo", orden.getSolicitud().getActivo() == null ? null : orden.getSolicitud().getActivo().getNombre());
        } else if (orden.getRequerimiento() != null) {
            variables.put("origen", "Requerimiento " + orden.getRequerimiento().getNumeroRequerimiento());
            variables.put("descripcion", orden.getRequerimiento().getDescripcion());
            if (orden.getRequerimiento().getSolicitud() != null) {
                variables.put("prioridad", orden.getRequerimiento().getSolicitud().getPrioridad());
            }
        }
        return variables;
    }
}
