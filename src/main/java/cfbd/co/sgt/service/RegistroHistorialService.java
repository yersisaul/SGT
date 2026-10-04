package cfbd.co.sgt.service;

import java.time.Instant;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.model.AsignacionOrden;
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.HistorialOrden;
import cfbd.co.sgt.model.HistorialRequerimiento;
import cfbd.co.sgt.model.HistorialSolicitud;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.model.TipoAsignacionOrden;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.AsignacionOrdenRepository;
import cfbd.co.sgt.repository.HistorialOrdenRepository;
import cfbd.co.sgt.repository.HistorialRequerimientoRepository;
import cfbd.co.sgt.repository.HistorialSolicitudRepository;
import lombok.RequiredArgsConstructor;

/**
 * Único punto que escribe la auditoría del flujo (historiales de estado y
 * eventos de asignación de OT). Se invoca dentro de la transacción de la
 * operación de negocio, con el usuario autenticado como actor.
 */
@Component
@RequiredArgsConstructor
public class RegistroHistorialService {

    private final HistorialSolicitudRepository historialSolicitudRepository;
    private final HistorialRequerimientoRepository historialRequerimientoRepository;
    private final HistorialOrdenRepository historialOrdenRepository;
    private final AsignacionOrdenRepository asignacionOrdenRepository;

    public void solicitud(Solicitud solicitud, Usuario actor, Estado anterior, Estado nuevo, String comentario) {
        HistorialSolicitud historial = new HistorialSolicitud();
        historial.setSolicitud(solicitud);
        historial.setUsuario(actor);
        historial.setEstado_anterior(anterior);
        historial.setEstado_nuevo(nuevo);
        historial.setFecha(Instant.now());
        historial.setComentario(comentario);
        historialSolicitudRepository.save(historial);
    }

    public void requerimiento(Requerimiento requerimiento, Usuario actor, Estado anterior, Estado nuevo, String comentario) {
        HistorialRequerimiento historial = new HistorialRequerimiento();
        historial.setRequerimiento(requerimiento);
        historial.setUsuario(actor);
        historial.setEstado_anterior(anterior);
        historial.setEstado_nuevo(nuevo);
        historial.setFecha(Instant.now());
        historial.setComentario(comentario);
        historialRequerimientoRepository.save(historial);
    }

    public void orden(Orden orden, Usuario actor, Estado anterior, Estado nuevo, String comentario) {
        HistorialOrden historial = new HistorialOrden();
        historial.setOrden(orden);
        historial.setUsuario(actor);
        historial.setEstado_anterior(anterior);
        historial.setEstado_nuevo(nuevo);
        historial.setFecha(Instant.now());
        historial.setComentario(comentario);
        historialOrdenRepository.save(historial);
    }

    public void asignacion(Orden orden, TipoAsignacionOrden tipo, Usuario actor, Especialidad especialidadOrigen,
                           Usuario usuarioOrigen, Usuario usuarioDestino, String motivo) {
        AsignacionOrden asignacion = new AsignacionOrden();
        asignacion.setOrden(orden);
        asignacion.setTipo(tipo);
        asignacion.setUsuario_actor(actor);
        asignacion.setEspecialidad_origen(especialidadOrigen);
        asignacion.setEspecialidad_destino(orden.getEspecialidad());
        asignacion.setUsuario_origen(usuarioOrigen);
        asignacion.setUsuario_destino(usuarioDestino);
        asignacion.setMotivo(motivo);
        asignacion.setFecha(Instant.now());
        asignacionOrdenRepository.save(asignacion);
    }
}
