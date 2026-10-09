package cfbd.co.sgt.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cfbd.co.sgt.dto.request.AsignarOrdenRequest;
import cfbd.co.sgt.dto.request.ReasignarOrdenRequest;
import cfbd.co.sgt.dto.request.VerificarOrdenRequest;
import cfbd.co.sgt.dto.response.AsignacionOrdenResponse;
import cfbd.co.sgt.dto.response.CargaMiembroResponse;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.mapper.OrdenMapper;
import cfbd.co.sgt.model.AsignacionOrden;
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.TipoAsignacionOrden;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.model.UsuarioEspecialidad;
import cfbd.co.sgt.repository.AsignacionOrdenRepository;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.repository.UsuarioEspecialidadRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.AutorizacionRecursoService;
import cfbd.co.sgt.service.ColaOrdenService;
import cfbd.co.sgt.service.EstadoResolver;
import cfbd.co.sgt.service.EstadosNegocio;
import cfbd.co.sgt.service.OrdenNotificableEvent;
import cfbd.co.sgt.service.RegistroHistorialService;
import cfbd.co.sgt.service.TipoNotificacion;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ColaOrdenServiceImpl implements ColaOrdenService {

    // "En progreso" no: el ejecutor ya verificó que le corresponde y la está
    // atendiendo (observación 2026-10-09).
    private static final Set<String> ESTADOS_REASIGNABLES = Set.of(
            EstadosNegocio.PENDIENTE, EstadosNegocio.ASIGNADA, EstadosNegocio.DEVUELTA);

    private final OrdenRepository ordenRepository;
    private final EspecialidadRepository especialidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioEspecialidadRepository usuarioEspecialidadRepository;
    private final AsignacionOrdenRepository asignacionOrdenRepository;
    private final UsuarioActualProvider usuarioActual;
    private final AutorizacionRecursoService autorizacion;
    private final EstadoResolver estados;
    private final RegistroHistorialService registro;
    private final OrdenMapper ordenMapper;
    private final ApplicationEventPublisher eventos;

    @Override
    @Transactional(readOnly = true)
    public List<OrdenResponse> listarCola() {
        List<UsuarioEspecialidad> pertenencias = usuarioEspecialidadRepository.findByUsuario(usuarioActual.obtener().getId_usuario());
        List<UUID> todas = new ArrayList<>();
        List<UUID> comoResponsable = new ArrayList<>();
        for (UsuarioEspecialidad pertenencia : pertenencias) {
            todas.add(pertenencia.getEspecialidad().getId_especialidad());
            if (Boolean.TRUE.equals(pertenencia.getEs_responsable())) {
                comoResponsable.add(pertenencia.getEspecialidad().getId_especialidad());
            }
        }
        List<Orden> cola = new ArrayList<>();
        if (!todas.isEmpty()) {
            cola.addAll(ordenRepository.findCola(todas, List.of(EstadosNegocio.PENDIENTE)));
        }
        if (!comoResponsable.isEmpty()) {
            cola.addAll(ordenRepository.findCola(comoResponsable, List.of(EstadosNegocio.DEVUELTA)));
        }
        return cola.stream().map(ordenMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CargaMiembroResponse> listarCargaEquipo(UUID idEspecialidad) {
        Especialidad especialidad = buscarEspecialidad(idEspecialidad);
        Usuario actor = usuarioActual.obtener();
        if (!autorizacion.esResponsable(actor, especialidad) && !autorizacion.veTodasLasOrdenes()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el responsable de la especialidad ve la carga del equipo.");
        }
        Map<UUID, Long> abiertas = new HashMap<>();
        for (Object[] fila : ordenRepository.contarAbiertasPorEjecutor(idEspecialidad)) {
            abiertas.put((UUID) fila[0], (Long) fila[1]);
        }
        return usuarioEspecialidadRepository.findByEspecialidad(idEspecialidad).stream()
                .map(pertenencia -> toCarga(pertenencia, abiertas.getOrDefault(pertenencia.getUsuario().getId_usuario(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public OrdenResponse tomar(UUID idOrden) {
        Usuario actor = usuarioActual.obtener();
        Orden orden = bloquear(idOrden, actor);
        if (!autorizacion.esMiembro(actor, orden.getEspecialidad())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un miembro de la especialidad puede tomar la Orden.");
        }
        exigirEnCola(orden);
        return asignarEjecutor(orden, actor, actor, TipoAsignacionOrden.TOMADA,
                "Tomada de la cola de " + orden.getEspecialidad().getNombre() + ".");
    }

    @Override
    @Transactional
    public OrdenResponse asignar(UUID idOrden, AsignarOrdenRequest request) {
        Usuario actor = usuarioActual.obtener();
        Orden orden = bloquear(idOrden, actor);
        if (!autorizacion.esResponsable(actor, orden.getEspecialidad())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el responsable de la especialidad puede asignar la Orden.");
        }
        if (orden.getUsuario() != null || !(EstadoResolver.es(orden.getEstado(), EstadosNegocio.PENDIENTE)
                || EstadoResolver.es(orden.getEstado(), EstadosNegocio.DEVUELTA))) {
            throw conflicto("La Orden no está en cola ni devuelta (estado: " + orden.getEstado().getNombre() + ").");
        }
        Usuario ejecutor = usuarioRepository.findById(request.getId_usuario())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario not found"));
        if (!autorizacion.esMiembro(ejecutor, orden.getEspecialidad())) {
            throw conflicto("El usuario no es miembro de la especialidad " + orden.getEspecialidad().getNombre() + ".");
        }
        return asignarEjecutor(orden, ejecutor, actor, TipoAsignacionOrden.ASIGNADA,
                "Asignada por el responsable a " + nombre(ejecutor) + ".");
    }

    @Override
    @Transactional
    public OrdenResponse verificar(UUID idOrden, VerificarOrdenRequest request) {
        Usuario actor = usuarioActual.obtener();
        Orden orden = bloquear(idOrden, actor);
        if (orden.getUsuario() == null || !orden.getUsuario().getId_usuario().equals(actor.getId_usuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el ejecutor asignado puede verificar la Orden.");
        }
        if (!EstadoResolver.es(orden.getEstado(), EstadosNegocio.ASIGNADA)) {
            throw conflicto("Solo se verifica una Orden 'Asignada' (estado: " + orden.getEstado().getNombre() + ").");
        }
        Estado anterior = orden.getEstado();
        if (Boolean.TRUE.equals(request.getCorresponde())) {
            Estado enProgreso = estados.porNombre(EstadosNegocio.EN_PROGRESO);
            orden.setEstado(enProgreso);
            Orden guardada = ordenRepository.save(orden);
            registro.orden(guardada, actor, anterior, enProgreso, "El ejecutor confirma que le corresponde.");
            registro.asignacion(guardada, TipoAsignacionOrden.CONFIRMADA, actor, null, actor, actor, request.getMotivo());
            return ordenMapper.toResponse(guardada);
        }
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique el motivo por el que la Orden no le corresponde.");
        }
        Estado devuelta = estados.porNombre(EstadosNegocio.DEVUELTA);
        orden.setUsuario(null);
        orden.setEstado(devuelta);
        Orden guardada = ordenRepository.save(orden);
        registro.orden(guardada, actor, anterior, devuelta, "Devuelta al responsable: " + request.getMotivo());
        registro.asignacion(guardada, TipoAsignacionOrden.DEVUELTA, actor, null, actor, null, request.getMotivo());
        notificar(TipoNotificacion.ORDEN_DEVUELTA, guardada, null);
        return ordenMapper.toResponse(guardada);
    }

    @Override
    @Transactional
    public OrdenResponse reasignar(UUID idOrden, ReasignarOrdenRequest request) {
        Usuario actor = usuarioActual.obtener();
        Orden orden = bloquear(idOrden, actor);
        Especialidad origen = orden.getEspecialidad();
        if (!autorizacion.esResponsable(actor, origen) && !autorizacion.veTodasLasOrdenes()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el responsable de la especialidad o un Administrador pueden reasignar la Orden.");
        }
        if (ESTADOS_REASIGNABLES.stream().noneMatch(nombre -> EstadoResolver.es(orden.getEstado(), nombre))) {
            throw conflicto("No se puede reasignar una Orden en estado '" + orden.getEstado().getNombre() + "'.");
        }
        Especialidad destino = buscarEspecialidad(request.getId_especialidad_destino());
        if (destino.getId_especialidad().equals(origen.getId_especialidad())) {
            throw conflicto("La Orden ya pertenece a la especialidad " + destino.getNombre() + ".");
        }
        Estado anterior = orden.getEstado();
        Usuario ejecutorAnterior = orden.getUsuario();
        Estado pendiente = estados.porNombre(EstadosNegocio.PENDIENTE);
        orden.setEspecialidad(destino);
        orden.setUsuario(null);
        orden.setEstado(pendiente);
        Orden guardada = ordenRepository.save(orden);
        registro.orden(guardada, actor, anterior, pendiente,
                "Reasignada de " + origen.getNombre() + " a " + destino.getNombre() + ": " + request.getMotivo());
        registro.asignacion(guardada, TipoAsignacionOrden.REASIGNADA_ESPECIALIDAD, actor, origen,
                ejecutorAnterior, null, request.getMotivo());
        notificar(TipoNotificacion.ORDEN_ENCOLADA, guardada, null);
        return ordenMapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionOrdenResponse> listarAsignaciones(UUID idOrden) {
        Orden orden = ordenRepository.findById(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found"));
        autorizacion.exigirVisible(usuarioActual.obtener(), orden);
        return asignacionOrdenRepository.findByOrden(idOrden).stream().map(this::toAsignacionResponse).toList();
    }

    private Orden bloquear(UUID idOrden, Usuario actor) {
        Orden orden = ordenRepository.findByIdParaActualizar(idOrden)
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found"));
        autorizacion.exigirVisible(actor, orden);
        if (orden.getFecha_cierre() != null) {
            throw conflicto("La Orden ya está cerrada.");
        }
        return orden;
    }

    private void exigirEnCola(Orden orden) {
        if (orden.getUsuario() != null || !EstadoResolver.es(orden.getEstado(), EstadosNegocio.PENDIENTE)) {
            throw conflicto("La Orden ya fue tomada o no está en la cola.");
        }
    }

    private OrdenResponse asignarEjecutor(Orden orden, Usuario ejecutor, Usuario actor, TipoAsignacionOrden tipo,
                                          String comentario) {
        Estado anterior = orden.getEstado();
        Estado asignada = estados.porNombre(EstadosNegocio.ASIGNADA);
        orden.setUsuario(ejecutor);
        orden.setEstado(asignada);
        Orden guardada = ordenRepository.save(orden);
        registro.orden(guardada, actor, anterior, asignada, comentario);
        registro.asignacion(guardada, tipo, actor, null, null, ejecutor, null);
        // Al tomarla, el propio actor no necesita aviso; al asignarla, sí el ejecutor.
        if (tipo == TipoAsignacionOrden.ASIGNADA) {
            notificar(TipoNotificacion.ORDEN_ASIGNADA, guardada, ejecutor.getId_usuario());
        }
        return ordenMapper.toResponse(guardada);
    }

    private void notificar(TipoNotificacion tipo, Orden orden, UUID idUsuarioDestino) {
        eventos.publishEvent(new OrdenNotificableEvent(tipo, orden.getId_orden(), orden.getNumeroOrden(),
                orden.getEspecialidad().getId_especialidad(), idUsuarioDestino));
    }

    private Especialidad buscarEspecialidad(UUID idEspecialidad) {
        return especialidadRepository.findById(idEspecialidad)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found"));
    }

    private ResponseStatusException conflicto(String mensaje) {
        return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private String nombre(Usuario usuario) {
        return (usuario.getNombres() + " " + usuario.getApellidos()).trim();
    }

    private CargaMiembroResponse toCarga(UsuarioEspecialidad pertenencia, long abiertas) {
        CargaMiembroResponse response = new CargaMiembroResponse();
        response.setId_usuario(pertenencia.getUsuario().getId_usuario());
        response.setNombres(pertenencia.getUsuario().getNombres());
        response.setApellidos(pertenencia.getUsuario().getApellidos());
        response.setEs_responsable(pertenencia.getEs_responsable());
        response.setOrdenes_abiertas(abiertas);
        return response;
    }

    private AsignacionOrdenResponse toAsignacionResponse(AsignacionOrden asignacion) {
        AsignacionOrdenResponse response = new AsignacionOrdenResponse();
        response.setId_asignacion_orden(asignacion.getId_asignacion_orden());
        response.setId_orden(asignacion.getOrden().getId_orden());
        response.setTipo(asignacion.getTipo().name());
        response.setId_especialidad_origen(asignacion.getEspecialidad_origen() != null
                ? asignacion.getEspecialidad_origen().getId_especialidad() : null);
        response.setId_especialidad_destino(asignacion.getEspecialidad_destino().getId_especialidad());
        response.setId_usuario_origen(asignacion.getUsuario_origen() != null ? asignacion.getUsuario_origen().getId_usuario() : null);
        response.setId_usuario_destino(asignacion.getUsuario_destino() != null ? asignacion.getUsuario_destino().getId_usuario() : null);
        response.setId_usuario_actor(asignacion.getUsuario_actor().getId_usuario());
        response.setNombre_actor(nombre(asignacion.getUsuario_actor()));
        response.setMotivo(asignacion.getMotivo());
        response.setFecha(asignacion.getFecha());
        return response;
    }
}
