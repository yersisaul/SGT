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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;

import cfbd.co.sgt.dto.request.GenerarOrdenRequest;
import cfbd.co.sgt.dto.request.GenerarRequerimientoRequest;
import cfbd.co.sgt.dto.request.SolicitudRequest;
import cfbd.co.sgt.dto.response.EstadoCantidadResponse;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.dto.response.RequerimientoResponse;
import cfbd.co.sgt.dto.response.ResumenEstadosResponse;
import cfbd.co.sgt.dto.response.SolicitudResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Activo;
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.ActivoRepository;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.HistorialRequerimientoRepository;
import cfbd.co.sgt.repository.HistorialSolicitudRepository;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.repository.RequerimientoRepository;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.AutorizacionRecursoService;
import cfbd.co.sgt.mapper.OrdenMapper;
import cfbd.co.sgt.service.EstadoResolver;
import cfbd.co.sgt.service.EstadosNegocio;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.GeneradorOrdenService;
import cfbd.co.sgt.service.RegistroHistorialService;
import cfbd.co.sgt.service.NumeracionService;
import cfbd.co.sgt.service.SlaCalculator;
import cfbd.co.sgt.service.SolicitudService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class SolicitudServiceImpl implements SolicitudService {

    // Transiciones válidas para Solicitud vía PUT genérico (nombre en
    // minúsculas). Solicitud maneja Pendiente/En revisión/En progreso/
    // Finalizado; ninguna transición es manual: desde "Pendiente" el
    // Despachador clasifica y despacha con una de dos acciones de negocio —
    // "En progreso" vía generarOrdenDesdeSolicitud (bajo contrato) o "En
    // revisión" vía generarRequerimientoDesdeSolicitud (fuera de contrato) —
    // y "Finalizado" solo se alcanza cuando se cierra la Orden asociada
    // (directamente, o vía el Requerimiento que se aprobó y generó OT), nunca por PUT genérico.
    private static final String ESTADO_PENDIENTE = "Pendiente";

    private static final Map<String, Set<String>> TRANSICIONES_PERMITIDAS = Map.of(
            "pendiente", Set.of(),
            "en revisión", Set.of(),
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
    private RequerimientoRepository requerimientoRepository;

    @Autowired
    private HistorialRequerimientoRepository historialRequerimientoRepository;

    @Autowired
    private SlaCalculator slaCalculator;

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private AutorizacionRecursoService autorizacion;
    @Autowired
    private NumeracionService numeracion;
    @Autowired
    private UsuarioActualProvider usuarioActual;
    @Autowired
    private GeneradorOrdenService generadorOrden;
    @Autowired
    private RegistroHistorialService registro;
    @Autowired
    private OrdenMapper ordenMapper;

    @Override
    public SolicitudResponse crearSolicitud(SolicitudRequest solicitudDTO) {
        Solicitud solicitud = new Solicitud();
        // El solicitante es el usuario autenticado, nunca un id enviado por el cliente.
        solicitud.setUsuario(usuarioAutenticado());
        Activo activo = activoRepository.findById(solicitudDTO.getId_activo())
                .orElseThrow(() -> new ResourceNotFoundException("Activo not found"));
        solicitud.setActivo(activo);
        // Toda Solicitud nace "Pendiente" para que el Despachador la revise se ignora cualquier id_estado del body.
        solicitud.setEstado(estadoPorNombre(ESTADO_PENDIENTE));
        // La especialidad inicial es la del activo (decisión 2026-10-03): el
        // Cliente no la elige; el Despachador la confirma o cambia al despachar.
        solicitud.setEspecialidad(activo.getEspecialidad());
        solicitud.setPrioridad(solicitudDTO.getPrioridad());
        solicitud.setDescripcion(solicitudDTO.getDescripcion());
        // El adjunto se gestiona exclusivamente vía subirAdjunto/eliminarAdjunto no se acepta desde este DTO.
        solicitud.setNumeroSolicitud(numeracion.siguienteNumeroSolicitud());
        solicitud.setFecha_registro(Instant.now());
        return convertToResponse(solicitudRepository.save(solicitud));
    }

    @Override
    public SolicitudResponse editarSolicitud(SolicitudRequest solicitudDTO, UUID id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found"));
        autorizacion.exigirVisible(usuarioActual.obtener(), solicitud);
        boolean pendiente = ESTADO_PENDIENTE.equalsIgnoreCase(solicitud.getEstado().getNombre());
        // Sin alcance global (p. ej. Cliente) solo se edita la propia y mientras siga "Pendiente".
        if (!autorizacion.veTodasLasSolicitudes() && !pendiente) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede editar la Solicitud mientras está 'Pendiente'.");
        }
        // El usuario solicitante es un hecho de negocio fijado en la creación;
        // no se reasigna desde una edición genérica.
        Estado estadoNuevo = solicitudDTO.getId_estado() == null
                ? solicitud.getEstado()
                : estadoRepository.findById(solicitudDTO.getId_estado())
                        .orElseThrow(() -> new ResourceNotFoundException("Estado not found"));
        validarTransicion(solicitud.getEstado(), estadoNuevo);
        Activo activo = activoRepository.findById(solicitudDTO.getId_activo())
                .orElseThrow(() -> new ResourceNotFoundException("Activo not found"));
        // Sin alcance global la especialidad sigue al activo; quien clasifica
        // (alcance global) puede indicarla explícitamente.
        Especialidad especialidad = autorizacion.veTodasLasSolicitudes() && solicitudDTO.getId_especialidad() != null
                ? especialidadRepository.findById(solicitudDTO.getId_especialidad())
                        .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found"))
                : activo.getEspecialidad();
        // Una vez despachada (OT o RQ generados) la especialidad y el activo
        // quedan fijos: la OT/RQ ya se generó con ellos.
        if (!pendiente && (!solicitud.getActivo().getId_activo().equals(activo.getId_activo())
                || !solicitud.getEspecialidad().getId_especialidad().equals(especialidad.getId_especialidad()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el activo ni la especialidad de una Solicitud ya despachada.");
        }
        solicitud.setActivo(activo);
        solicitud.setEstado(estadoNuevo);
        solicitud.setEspecialidad(especialidad);
        solicitud.setPrioridad(solicitudDTO.getPrioridad());
        solicitud.setDescripcion(solicitudDTO.getDescripcion());
        return convertToResponse(solicitudRepository.save(solicitud));
    }

    @Override
    public List<SolicitudResponse> listarSolicitudes() {
        return solicitudesVisibles().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SolicitudResponse> buscarSolicitudPorId(UUID id) {
        Usuario actor = usuarioAutenticado();
        return solicitudRepository.findById(id)
                .filter(solicitud -> autorizacion.puedeVer(actor, solicitud))
                .map(this::convertToResponse);
    }

    @Override
    public Optional<SolicitudResponse> buscarSolicitudPorNumero(String numeroSolicitud) {
        Usuario actor = usuarioAutenticado();
        return solicitudRepository.findByNumeroSolicitud(numeroSolicitud)
                .filter(solicitud -> autorizacion.puedeVer(actor, solicitud))
                .map(this::convertToResponse);
    }

    @Override
    public void eliminarSolicitud(UUID id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found"));
        // Sin alcance global solo se borra lo que el actor puede ver (CLAUDE §6.5).
        autorizacion.exigirVisible(usuarioActual.obtener(), solicitud);
        // Una Solicitud con historial, Requerimiento u OT es parte de la
        // auditoría del flujo: no se borra físicamente.
        if (!ESTADO_PENDIENTE.equalsIgnoreCase(solicitud.getEstado().getNombre())
                || historialSolicitudRepository.existsByPadre(id)
                || requerimientoRepository.existsBySolicitud(id)
                || ordenRepository.existsPorSolicitudDirectaOIndirecta(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar una Solicitud que ya fue despachada o tiene historial.");
        }
        solicitudRepository.delete(solicitud);
    }

    @Override
    public ResumenEstadosResponse obtenerResumenPorEstado() {
        Map<String, Long> conteosPorEstado = new HashMap<>();
        long total;
        if (autorizacion.veTodasLasSolicitudes()) {
            for (Object[] fila : solicitudRepository.countByEstado()) {
                conteosPorEstado.put((String) fila[0], (Long) fila[1]);
            }
            total = solicitudRepository.count();
        } else {
            List<Solicitud> visibles = solicitudesVisibles();
            visibles.forEach(s -> conteosPorEstado.merge(s.getEstado().getNombre(), 1L, Long::sum));
            total = visibles.size();
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
        response.setTotal(total);
        response.setPorEstado(porEstado);
        return response;
    }

    @Override
    public OrdenResponse generarOrdenDesdeSolicitud(UUID idSolicitud, GenerarOrdenRequest request) {
        Solicitud solicitud = solicitudPendienteParaDespachar(idSolicitud, "una Orden");
        if (ordenRepository.existsBySolicitud(idSolicitud)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una Orden generada para esta Solicitud.");
        }
        Usuario actor = usuarioAutenticado();
        // Paso 5 (bajo contrato): el Despachador confirma o cambia la
        // especialidad (PRD D22) y la OT entra a la cola de esa especialidad,
        // sin ejecutor: la toma un miembro o la asigna el responsable (D3).
        Especialidad especialidad = especialidadElegida(request.getId_especialidad(), solicitud);
        solicitud.setEspecialidad(especialidad);
        Orden orden = generadorOrden.generarEnCola(especialidad, solicitud, null, actor, request.getComentario());

        // Generar la OT NO finaliza la atención: la Solicitud pasa a "En
        // progreso" y solo llega a "Finalizado" cuando la Orden se cierra.
        Estado anterior = solicitud.getEstado();
        Estado enProgreso = estadoPorNombre(EstadosNegocio.EN_PROGRESO);
        solicitud.setEstado(enProgreso);
        solicitudRepository.save(solicitud);
        registro.solicitud(solicitud, actor, anterior, enProgreso, request.getComentario() != null
                ? request.getComentario()
                : "Orden de trabajo " + orden.getNumeroOrden() + " generada (bajo contrato) en la cola de "
                        + especialidad.getNombre() + "; Solicitud pasa a En progreso.");
        return ordenMapper.toResponse(orden);
    }

    @Override
    public RequerimientoResponse generarRequerimientoDesdeSolicitud(UUID idSolicitud, GenerarRequerimientoRequest request) {
        // Mismo punto de entrada que generarOrdenDesdeSolicitud: el
        // Despachador clasifica una Solicitud Pendiente en uno de dos
        // caminos (bajo contrato -> OT, fuera de contrato -> Requerimiento),
        // nunca ambos ni repetido.
        Solicitud solicitud = solicitudPendienteParaDespachar(idSolicitud, "un Requerimiento");
        Usuario actor = usuarioAutenticado();
        Especialidad especialidad = especialidadElegida(request != null ? request.getId_especialidad() : null, solicitud);
        solicitud.setEspecialidad(especialidad);
        String descripcion = request != null && request.getDescripcion() != null && !request.getDescripcion().isBlank()
                ? request.getDescripcion()
                : descripcionRequerimientoDesdeSolicitud(solicitud);

        Estado enRevision = estadoPorNombre(EstadosNegocio.EN_REVISION);
        Requerimiento requerimiento = new Requerimiento();
        requerimiento.setUsuario(actor);
        // Un Requerimiento nuevo nace directamente "En revisión" (paso 7).
        requerimiento.setEstado(enRevision);
        requerimiento.setEspecialidad(especialidad);
        requerimiento.setSolicitud(solicitud);
        requerimiento.setDescripcion(descripcion);
        requerimiento.setNumeroRequerimiento(numeracion.siguienteNumeroRequerimiento());
        requerimiento.setFecha_registro(Instant.now());
        Requerimiento guardado = requerimientoRepository.save(requerimiento);
        registro.requerimiento(guardado, actor, enRevision, enRevision,
                "Requerimiento creado desde la Solicitud " + solicitud.getNumeroSolicitud() + " (fuera de contrato).");

        Estado anterior = solicitud.getEstado();
        solicitud.setEstado(enRevision);
        solicitudRepository.save(solicitud);
        registro.solicitud(solicitud, actor, anterior, enRevision, "Requerimiento " + guardado.getNumeroRequerimiento()
                + " generado desde la Solicitud (fuera de contrato); Solicitud pasa a En revisión.");
        return convertirRequerimientoAResponse(guardado);
    }

    /** Solo se despacha (OT o RQ) una Solicitud visible y en "Pendiente". */
    private Solicitud solicitudPendienteParaDespachar(UUID idSolicitud, String destino) {
        Solicitud solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found"));
        autorizacion.exigirVisible(usuarioAutenticado(), solicitud);
        if (!EstadoResolver.es(solicitud.getEstado(), EstadosNegocio.PENDIENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede generar " + destino + " desde una Solicitud en estado 'Pendiente' (actual: "
                            + solicitud.getEstado().getNombre() + ").");
        }
        return solicitud;
    }

    private Especialidad especialidadElegida(UUID idEspecialidad, Solicitud solicitud) {
        if (idEspecialidad == null) {
            return solicitud.getEspecialidad();
        }
        return especialidadRepository.findById(idEspecialidad)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found"));
    }

    @Override
    public SolicitudResponse subirAdjunto(UUID id, MultipartFile file) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found"));
        if (!autorizacion.puedeVer(usuarioActual.obtener(), solicitud)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta Solicitud.");
        }
        String referenciaAnterior = solicitud.getUrl_adjunto();
        String nuevaReferencia = fileStorageService.store(file, TipoRecursoArchivo.SOLICITUDES, id);
        solicitud.setUrl_adjunto(nuevaReferencia);
        Solicitud guardada = solicitudRepository.save(solicitud);
        if (referenciaAnterior != null) {
            fileStorageService.delete(referenciaAnterior);
        }
        return convertToResponse(guardada);
    }

    @Override
    public String obtenerReferenciaAdjunto(UUID id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found"));
        if (!autorizacion.puedeVer(usuarioActual.obtener(), solicitud)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta Solicitud.");
        }
        if (solicitud.getUrl_adjunto() == null) {
            throw new ResourceNotFoundException("La Solicitud no tiene adjunto.");
        }
        return solicitud.getUrl_adjunto();
    }

    @Override
    public SolicitudResponse eliminarAdjunto(UUID id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found"));
        if (!autorizacion.puedeVer(usuarioActual.obtener(), solicitud)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta Solicitud.");
        }
        if (solicitud.getUrl_adjunto() != null) {
            fileStorageService.delete(solicitud.getUrl_adjunto());
            solicitud.setUrl_adjunto(null);
            solicitudRepository.save(solicitud);
        }
        return convertToResponse(solicitud);
    }

    private String descripcionRequerimientoDesdeSolicitud(Solicitud solicitud) {
        return solicitud.getDescripcion()
                + "\n\nRequerimiento generado a partir de la Solicitud " + solicitud.getNumeroSolicitud() + ".";
    }

    private List<Solicitud> solicitudesVisibles() {
        return autorizacion.veTodasLasSolicitudes()
                ? solicitudRepository.findAll()
                : solicitudRepository.findVisiblesPara(usuarioActual.obtener().getId_usuario());
    }

    private Estado estadoPorNombre(String nombre) {
        return estadoRepository.findByNombre(nombre)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Estado '" + nombre + "' no está configurado en el catálogo."));
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
        return usuarioActual.obtener();
    }

    private SolicitudResponse convertToResponse(Solicitud solicitud) {
        SolicitudResponse response = new SolicitudResponse();
        response.setId_solicitud(solicitud.getId_solicitud());
        response.setId_usuario(solicitud.getUsuario().getId_usuario());
        response.setNombre_usuario(nombreCompleto(solicitud.getUsuario()));
        response.setId_activo(solicitud.getActivo().getId_activo());
        response.setId_estado(solicitud.getEstado().getId_estado());
        response.setId_especialidad(solicitud.getEspecialidad().getId_especialidad());
        response.setNumeroSolicitud(solicitud.getNumeroSolicitud());
        response.setPrioridad(solicitud.getPrioridad());
        response.setFecha_registro(solicitud.getFecha_registro());
        response.setDescripcion(solicitud.getDescripcion());
        response.setUrl_adjunto(solicitud.getUrl_adjunto() != null
                ? "/api/archivos/solicitudes/" + solicitud.getId_solicitud() : null);
        response.setFecha_limite_despacho(
                slaCalculator.deadlineSolicitud(solicitud.getFecha_registro(), solicitud.getPrioridad()));
        return response;
    }

    private String nombreCompleto(Usuario usuario) {
        return usuario == null ? null : (usuario.getNombres() + " " + usuario.getApellidos()).trim();
    }

    private RequerimientoResponse convertirRequerimientoAResponse(Requerimiento requerimiento) {
        RequerimientoResponse response = new RequerimientoResponse();
        response.setId_requerimiento(requerimiento.getId_requerimiento());
        response.setId_usuario(requerimiento.getUsuario().getId_usuario());
        response.setId_estado(requerimiento.getEstado().getId_estado());
        response.setId_especialidad(requerimiento.getEspecialidad().getId_especialidad());
        response.setId_solicitud(requerimiento.getSolicitud() != null ? requerimiento.getSolicitud().getId_solicitud() : null);
        response.setNumeroRequerimiento(requerimiento.getNumeroRequerimiento());
        response.setFecha_registro(requerimiento.getFecha_registro());
        response.setDescripcion(requerimiento.getDescripcion());
        response.setUrl_adjunto(requerimiento.getUrl_adjunto() != null
                ? "/api/archivos/requerimientos/" + requerimiento.getId_requerimiento() : null);
        response.setFecha_limite_despacho(slaCalculator.deadlineRequerimiento(requerimiento.getFecha_registro()));
        return response;
    }
}
