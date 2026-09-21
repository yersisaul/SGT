package cfbd.co.sgt.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;

import cfbd.co.sgt.dto.request.GenerarOrdenRequest;
import cfbd.co.sgt.dto.request.SolicitudRequest;
import cfbd.co.sgt.dto.response.EstadoCantidadResponse;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.dto.response.ResumenEstadosResponse;
import cfbd.co.sgt.dto.response.SolicitudResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.HistorialOrden;
import cfbd.co.sgt.model.HistorialSolicitud;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.ActivoRepository;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.HistorialOrdenRepository;
import cfbd.co.sgt.repository.HistorialSolicitudRepository;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.service.SlaCalculator;
import cfbd.co.sgt.service.SolicitudService;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class SolicitudServiceImpl implements SolicitudService {

    // Transiciones válidas para Solicitud vía PUT genérico (nombre en
    // minúsculas). Solicitud solo maneja Pendiente/En progreso/Finalizado; no
    // hay ninguna transición manual: "Pendiente -> En progreso" solo se
    // alcanza despachando (generarOrdenDesdeSolicitud) y "-> Finalizado" solo
    // al cerrarse la Orden asociada (OrdenServiceImpl.cerrarOrden), nunca por
    // PUT genérico (CLAUDE.md 5.3).
    private static final Map<String, Set<String>> TRANSICIONES_PERMITIDAS = Map.of(
            "pendiente", Set.of(),
            "en progreso", Set.of(),
            "finalizado", Set.of());

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private HistorialSolicitudRepository historialSolicitudRepository;

    @Autowired
    private HistorialOrdenRepository historialOrdenRepository;

    @Autowired
    private SlaCalculator slaCalculator;

    @Override
    public SolicitudResponse crearSolicitud(SolicitudRequest solicitudDTO) {
        Solicitud solicitud = new Solicitud();
        // El solicitante es el usuario autenticado, nunca un id enviado por
        // el cliente (CLAUDE.md 6.1/5.1; mismo patrón de AprobacionServiceImpl).
        solicitud.setUsuario(usuarioAutenticado());
        solicitud.setActivo(activoRepository.findById(solicitudDTO.getId_activo())
                .orElseThrow(() -> new ResourceNotFoundException("Activo not found")));
        solicitud.setEstado(estadoRepository.findById(solicitudDTO.getId_estado())
                .orElseThrow(() -> new ResourceNotFoundException("Estado not found")));
        solicitud.setEspecialidad(especialidadRepository.findById(solicitudDTO.getId_especialidad())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found")));
        solicitud.setPrioridad(solicitudDTO.getPrioridad());
        solicitud.setDescripcion(solicitudDTO.getDescripcion());
        solicitud.setUrl_adjunto(solicitudDTO.getUrl_adjunto());
        Long correlativo = solicitudRepository.count() + 1;
        solicitud.setNumeroSolicitud("ST-" + correlativo);
        solicitud.setFecha_registro(Instant.now());
        return convertToResponse(solicitudRepository.save(solicitud));
    }

    @Override
    public SolicitudResponse editarSolicitud(SolicitudRequest solicitudDTO, UUID id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found"));
        // El usuario solicitante es un hecho de negocio fijado en la creación;
        // no se reasigna desde una edición genérica.
        Estado estadoNuevo = estadoRepository.findById(solicitudDTO.getId_estado())
                .orElseThrow(() -> new ResourceNotFoundException("Estado not found"));
        validarTransicion(solicitud.getEstado(), estadoNuevo);
        solicitud.setActivo(activoRepository.findById(solicitudDTO.getId_activo())
                .orElseThrow(() -> new ResourceNotFoundException("Activo not found")));
        solicitud.setEstado(estadoNuevo);
        solicitud.setEspecialidad(especialidadRepository.findById(solicitudDTO.getId_especialidad())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found")));
        solicitud.setPrioridad(solicitudDTO.getPrioridad());
        solicitud.setDescripcion(solicitudDTO.getDescripcion());
        solicitud.setUrl_adjunto(solicitudDTO.getUrl_adjunto());
        return convertToResponse(solicitudRepository.save(solicitud));
    }

    @Override
    public List<SolicitudResponse> listarSolicitudes() {
        Usuario actor = usuarioAutenticado();
        List<Solicitud> solicitudes = esCliente(actor)
                ? solicitudRepository.findByUsuario(actor.getId_usuario())
                : solicitudRepository.findAll();
        return solicitudes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SolicitudResponse> buscarSolicitudPorId(UUID id) {
        Usuario actor = usuarioAutenticado();
        return solicitudRepository.findById(id)
                .filter(solicitud -> puedeVer(actor, solicitud))
                .map(this::convertToResponse);
    }

    @Override
    public Optional<SolicitudResponse> buscarSolicitudPorNumero(String numeroSolicitud) {
        Usuario actor = usuarioAutenticado();
        return solicitudRepository.findByNumeroSolicitud(numeroSolicitud)
                .filter(solicitud -> puedeVer(actor, solicitud))
                .map(this::convertToResponse);
    }

    @Override
    public void eliminarSolicitud(UUID id) {
        solicitudRepository.deleteById(id);
    }

    @Override
    public ResumenEstadosResponse obtenerResumenPorEstado() {
        Map<String, Long> conteosPorEstado = new HashMap<>();
        for (Object[] fila : solicitudRepository.countByEstado()) {
            conteosPorEstado.put((String) fila[0], (Long) fila[1]);
        }

        List<EstadoCantidadResponse> porEstado = estadoRepository.findAll().stream()
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .map(estado -> {
                    EstadoCantidadResponse item = new EstadoCantidadResponse();
                    item.setEstado(estado.getNombre());
                    item.setCantidad(conteosPorEstado.getOrDefault(estado.getNombre(), 0L));
                    return item;
                })
                .collect(Collectors.toList());

        ResumenEstadosResponse response = new ResumenEstadosResponse();
        response.setTotal(solicitudRepository.count());
        response.setPorEstado(porEstado);
        return response;
    }

    @Override
    public OrdenResponse generarOrdenDesdeSolicitud(UUID idSolicitud, GenerarOrdenRequest request) {
        Solicitud solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found"));

        if ("Finalizado".equalsIgnoreCase(solicitud.getEstado().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La Solicitud ya fue finalizada; no se puede generar otra Orden desde ella.");
        }
        if (ordenRepository.existsBySolicitud(idSolicitud)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una Orden generada para esta Solicitud.");
        }

        Estado estadoAnterior = solicitud.getEstado();
        // Generar la OT NO finaliza la atención: la Solicitud pasa a "En
        // progreso" mientras la Orden avanza, y solo llega a "Finalizado"
        // cuando la Orden se cierra (ver OrdenServiceImpl.cerrarOrden).
        Estado estadoEnProgreso = estadoRepository.findByNombre("En progreso")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Estado 'En progreso' no está configurado en el catálogo."));
        Estado estadoInicialOrden = estadoRepository.findByNombre("Pendiente")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Estado 'Pendiente' no está configurado en el catálogo."));
        Usuario actor = usuarioAutenticado();
        Usuario ejecutor = resolverEjecutorOperaciones(request.getId_usuario_ejecutor());

        Orden orden = new Orden();
        // Orden.usuario es el ejecutor de Operaciones responsable, no quien
        // genera la OT (ver historial de creación más abajo para conservar
        // esa trazabilidad).
        orden.setUsuario(ejecutor);
        orden.setEstado(estadoInicialOrden);
        orden.setEspecialidad(solicitud.getEspecialidad());
        orden.setSolicitud(solicitud);
        orden.setRequerimiento(null);
        orden.setUrl_adjunto(request.getUrl_adjunto());
        Long correlativo = ordenRepository.count() + 1;
        orden.setNumeroOrden("OT-" + correlativo);
        orden.setFecha_registro(Instant.now());
        Orden ordenGuardada = ordenRepository.save(orden);

        HistorialOrden historialCreacion = new HistorialOrden();
        historialCreacion.setOrden(ordenGuardada);
        historialCreacion.setUsuario(actor);
        historialCreacion.setEstado_anterior(estadoInicialOrden);
        historialCreacion.setEstado_nuevo(estadoInicialOrden);
        historialCreacion.setFecha(Instant.now());
        historialCreacion.setComentario("Orden creada desde la Solicitud " + solicitud.getNumeroSolicitud()
                + " y asignada a " + ejecutor.getNombres() + " " + ejecutor.getApellidos() + ".");
        historialOrdenRepository.save(historialCreacion);

        solicitud.setEstado(estadoEnProgreso);
        solicitudRepository.save(solicitud);

        HistorialSolicitud historial = new HistorialSolicitud();
        historial.setSolicitud(solicitud);
        historial.setUsuario(actor);
        historial.setEstado_anterior(estadoAnterior);
        historial.setEstado_nuevo(estadoEnProgreso);
        historial.setFecha(Instant.now());
        historial.setComentario(request.getComentario() != null
                ? request.getComentario()
                : "Orden de trabajo " + ordenGuardada.getNumeroOrden() + " generada desde la Solicitud (bajo contrato); Solicitud pasa a En progreso.");
        historialSolicitudRepository.save(historial);

        return convertirOrdenAResponse(ordenGuardada);
    }

    /** Valida que el usuario elegido como ejecutor exista y tenga rol
     * Operaciones (CLAUDE.md: no permitir asignar una OT a alguien que no sea
     * Operaciones). */
    private Usuario resolverEjecutorOperaciones(UUID idUsuarioEjecutor) {
        Usuario ejecutor = usuarioRepository.findById(idUsuarioEjecutor)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario ejecutor no encontrado"));
        if (!"Operaciones".equalsIgnoreCase(ejecutor.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El ejecutor asignado debe tener rol Operaciones.");
        }
        return ejecutor;
    }

    private boolean esCliente(Usuario usuario) {
        return "Cliente".equalsIgnoreCase(usuario.getRol().getNombre());
    }

    private boolean puedeVer(Usuario actor, Solicitud solicitud) {
        return !esCliente(actor) || solicitud.getUsuario().getId_usuario().equals(actor.getId_usuario());
    }

    private void validarTransicion(Estado actual, Estado nuevo) {
        if (actual.getId_estado().equals(nuevo.getId_estado())) {
            return;
        }
        Set<String> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(actual.getNombre().toLowerCase(), Set.of());
        if (!permitidos.contains(nuevo.getNombre().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transición de estado no permitida para Solicitud: "
                            + actual.getNombre() + " -> " + nuevo.getNombre());
        }
    }

    private Usuario usuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private SolicitudResponse convertToResponse(Solicitud solicitud) {
        SolicitudResponse response = new SolicitudResponse();
        response.setId_solicitud(solicitud.getId_solicitud());
        response.setId_usuario(solicitud.getUsuario().getId_usuario());
        response.setId_activo(solicitud.getActivo().getId_activo());
        response.setId_estado(solicitud.getEstado().getId_estado());
        response.setId_especialidad(solicitud.getEspecialidad().getId_especialidad());
        response.setNumeroSolicitud(solicitud.getNumeroSolicitud());
        response.setPrioridad(solicitud.getPrioridad());
        response.setFecha_registro(solicitud.getFecha_registro());
        response.setDescripcion(solicitud.getDescripcion());
        response.setUrl_adjunto(solicitud.getUrl_adjunto());
        response.setFecha_limite_despacho(
                slaCalculator.deadlineSolicitud(solicitud.getFecha_registro(), solicitud.getPrioridad()));
        return response;
    }

    private OrdenResponse convertirOrdenAResponse(Orden orden) {
        OrdenResponse response = new OrdenResponse();
        response.setId_orden(orden.getId_orden());
        response.setId_usuario(orden.getUsuario().getId_usuario());
        response.setId_especialidad(orden.getEspecialidad().getId_especialidad());
        response.setId_estado(orden.getEstado().getId_estado());
        response.setId_requerimiento(orden.getRequerimiento() != null ? orden.getRequerimiento().getId_requerimiento() : null);
        response.setId_solicitud(orden.getSolicitud() != null ? orden.getSolicitud().getId_solicitud() : null);
        response.setNumeroOrden(orden.getNumeroOrden());
        response.setFecha_registro(orden.getFecha_registro());
        response.setFecha_cierre(orden.getFecha_cierre());
        response.setUrl_adjunto(orden.getUrl_adjunto());
        return response;
    }
}
