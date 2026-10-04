package cfbd.co.sgt.service.impl;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.model.TipoAsignacionOrden;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.service.EstadoResolver;
import cfbd.co.sgt.service.EstadosNegocio;
import cfbd.co.sgt.service.GeneradorOrdenService;
import cfbd.co.sgt.service.NumeracionService;
import cfbd.co.sgt.service.OrdenNotificableEvent;
import cfbd.co.sgt.service.TipoNotificacion;
import cfbd.co.sgt.service.RegistroHistorialService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeneradorOrdenServiceImpl implements GeneradorOrdenService {

    private final OrdenRepository ordenRepository;
    private final EstadoResolver estados;
    private final NumeracionService numeracion;
    private final RegistroHistorialService registro;
    private final ApplicationEventPublisher eventos;

    @Override
    public Orden generarEnCola(Especialidad especialidad, Solicitud solicitud, Requerimiento requerimiento,
                               Usuario actor, String comentario) {
        Estado pendiente = estados.porNombre(EstadosNegocio.PENDIENTE);
        Orden orden = new Orden();
        orden.setUsuario(null);
        orden.setEstado(pendiente);
        orden.setEspecialidad(especialidad);
        orden.setSolicitud(solicitud);
        orden.setRequerimiento(requerimiento);
        orden.setNumeroOrden(numeracion.siguienteNumeroOrden());
        orden.setFecha_registro(Instant.now());
        Orden guardada = ordenRepository.save(orden);

        String origen = solicitud != null
                ? "la Solicitud " + solicitud.getNumeroSolicitud()
                : "el Requerimiento " + requerimiento.getNumeroRequerimiento();
        String detalle = "Orden creada desde " + origen + " y encolada en " + especialidad.getNombre() + ".";
        registro.orden(guardada, actor, pendiente, pendiente,
                comentario != null && !comentario.isBlank() ? comentario + " — " + detalle : detalle);
        registro.asignacion(guardada, TipoAsignacionOrden.ENCOLADA, actor, null, null, null, comentario);
        eventos.publishEvent(new OrdenNotificableEvent(TipoNotificacion.ORDEN_ENCOLADA, guardada.getId_orden(),
                guardada.getNumeroOrden(), especialidad.getId_especialidad(), null));
        return guardada;
    }
}
