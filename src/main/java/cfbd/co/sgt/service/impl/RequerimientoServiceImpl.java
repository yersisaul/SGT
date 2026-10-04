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
import cfbd.co.sgt.dto.request.RequerimientoRequest;
import cfbd.co.sgt.dto.response.EstadoCantidadResponse;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.dto.response.RequerimientoResponse;
import cfbd.co.sgt.dto.response.ResumenEstadosResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.HistorialRequerimiento;
import cfbd.co.sgt.model.HistorialSolicitud;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.model.Usuario;
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
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.service.EstadoResolver;
import cfbd.co.sgt.service.EstadosNegocio;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.GeneradorOrdenService;
import cfbd.co.sgt.service.RegistroHistorialService;
import cfbd.co.sgt.service.NumeracionService;
import cfbd.co.sgt.service.RequerimientoService;
import cfbd.co.sgt.service.SlaCalculator;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class RequerimientoServiceImpl implements RequerimientoService {

    private static final Map<String, Set<String>> TRANSICIONES_PERMITIDAS = Map.of(
            "pendiente", Set.of("en revisión"),
            "en revisión", Set.of("pendiente"),
            "aprobado", Set.of(),
            "rechazado", Set.of(),
            "en progreso", Set.of(),
            "finalizado", Set.of());

    @Autowired
    private RequerimientoRepository requerimientoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private HistorialRequerimientoRepository historialRequerimientoRepository;


    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private HistorialSolicitudRepository historialSolicitudRepository;

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
    private EstadoResolver estados;
    @Autowired
    private OrdenMapper ordenMapper;

    @Override
    public RequerimientoResponse crearRequerimiento(RequerimientoRequest requerimientoDTO) {
        Requerimiento requerimiento = new Requerimiento();
        // El usuario se obtiene del contexto de seguridad, nunca del body.
        requerimiento.setUsuario(usuarioAutenticado());
        // Un Requerimiento nuevo nunca inicia en "Pendiente": el id_estado
        // del body se ignora deliberadamente (igual que generarOrdenDesde*
        // ignora el estado inicial de la Orden) y siempre nace "En revisión".
        Estado estadoInicial = estadoRepository.findByNombre("En revisión")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Estado 'En revisión' no está configurado en el catálogo."));
        requerimiento.setEstado(estadoInicial);
        requerimiento.setEspecialidad(especialidadRepository.findById(requerimientoDTO.getId_especialidad())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found")));
        requerimiento.setDescripcion(requerimientoDTO.getDescripcion());
        // El adjunto se gestiona exclusivamente vía subirAdjunto/eliminarAdjunto
        // (fileserver propio, CLAUDE.md sección 26/30): no se acepta desde este DTO.
        requerimiento.setNumeroRequerimiento(numeracion.siguienteNumeroRequerimiento());
        requerimiento.setFecha_registro(Instant.now());
        Requerimiento requerimientoGuardado = requerimientoRepository.save(requerimiento);

        HistorialRequerimiento historialCreacion = new HistorialRequerimiento();
        historialCreacion.setRequerimiento(requerimientoGuardado);
        historialCreacion.setUsuario(requerimientoGuardado.getUsuario());
        historialCreacion.setEstado_anterior(estadoInicial);
        historialCreacion.setEstado_nuevo(estadoInicial);
        historialCreacion.setFecha(Instant.now());
        historialCreacion.setComentario("Requerimiento creado.");
        historialRequerimientoRepository.save(historialCreacion);

        return convertToResponse(requerimientoGuardado);
    }

    @Override
    public RequerimientoResponse editarRequerimiento(RequerimientoRequest requerimientoDTO, UUID id) {
        Requerimiento requerimiento = requerimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found"));
        autorizacion.exigirVisible(usuarioActual.obtener(), requerimiento);
        String actualNombre = requerimiento.getEstado().getNombre().toLowerCase();
        if (actualNombre.equals("aprobado") || actualNombre.equals("rechazado")
                || actualNombre.equals("en progreso") || actualNombre.equals("finalizado")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede editar un Requerimiento en estado '" + requerimiento.getEstado().getNombre() + "'.");
        }

        Estado estadoNuevo = estadoRepository.findById(requerimientoDTO.getId_estado())
                .orElseThrow(() -> new ResourceNotFoundException("Estado not found"));
        validarTransicion(requerimiento.getEstado(), estadoNuevo);

        // El usuario que originó el Requerimiento no se reasigna por edición genérica.
        requerimiento.setEstado(estadoNuevo);
        requerimiento.setEspecialidad(especialidadRepository.findById(requerimientoDTO.getId_especialidad())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found")));
        requerimiento.setDescripcion(requerimientoDTO.getDescripcion());
        return convertToResponse(requerimientoRepository.save(requerimiento));
    }

    @Override
    public List<RequerimientoResponse> listarRequerimientos() {
        return requerimientosVisibles().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RequerimientoResponse> buscarRequerimientoPorId(UUID id) {
        Usuario actor = usuarioActual.obtener();
        return requerimientoRepository.findById(id)
                .filter(requerimiento -> autorizacion.puedeVer(actor, requerimiento))
                .map(this::convertToResponse);
    }

    @Override
    public Optional<RequerimientoResponse> buscarRequerimientoPorNumero(String numeroRequerimiento) {
        Usuario actor = usuarioActual.obtener();
        return requerimientoRepository.findByNumeroRequerimiento(numeroRequerimiento)
                .filter(requerimiento -> autorizacion.puedeVer(actor, requerimiento))
                .map(this::convertToResponse);
    }

    @Override
    public void eliminarRequerimiento(UUID id) {
        Requerimiento requerimiento = requerimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found"));
        // Con historial, aprobación u OT forma parte de la auditoría del flujo.
        if (historialRequerimientoRepository.existsByPadre(id)
                || !requerimiento.getAprobaciones().isEmpty()
                || ordenRepository.existsByRequerimiento(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar un Requerimiento con historial, aprobación u Orden asociada.");
        }
        requerimientoRepository.delete(requerimiento);
    }

    @Override
    public ResumenEstadosResponse obtenerResumenPorEstado() {
        Map<String, Long> conteosPorEstado = new HashMap<>();
        long total;
        if (autorizacion.veTodosLosRequerimientos()) {
            for (Object[] fila : requerimientoRepository.countByEstado()) {
                conteosPorEstado.put((String) fila[0], (Long) fila[1]);
            }
            total = requerimientoRepository.count();
        } else {
            List<Requerimiento> visibles = requerimientosVisibles();
            visibles.forEach(r -> conteosPorEstado.merge(r.getEstado().getNombre(), 1L, Long::sum));
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
    public OrdenResponse generarOrdenDesdeRequerimiento(UUID idRequerimiento, GenerarOrdenRequest request) {
        Requerimiento requerimiento = requerimientoRepository.findById(idRequerimiento)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found"));
        if (!EstadoResolver.es(requerimiento.getEstado(), EstadosNegocio.APROBADO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede generar una Orden desde un Requerimiento en estado 'Aprobado' (actual: "
                            + requerimiento.getEstado().getNombre() + ").");
        }
        if (ordenRepository.existsByRequerimiento(idRequerimiento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una Orden generada para este Requerimiento.");
        }
        Usuario actor = usuarioAutenticado();
        // Paso 9: el Administrador elige la especialidad (entre las 5, PRD
        // D16) y la OT entra a su cola sin ejecutor.
        Especialidad especialidad = especialidadRepository.findById(request.getId_especialidad())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found"));
        requerimiento.setEspecialidad(especialidad);
        Orden orden = generadorOrden.generarEnCola(especialidad, null, requerimiento, actor, request.getComentario());

        Estado anterior = requerimiento.getEstado();
        Estado enProgreso = estados.porNombre(EstadosNegocio.EN_PROGRESO);
        requerimiento.setEstado(enProgreso);
        requerimientoRepository.save(requerimiento);
        registro.requerimiento(requerimiento, actor, anterior, enProgreso, request.getComentario() != null
                ? request.getComentario()
                : "Orden de trabajo " + orden.getNumeroOrden() + " generada en la cola de " + especialidad.getNombre()
                        + "; Requerimiento pasa a En progreso.");

        // La Solicitud de origen (fuera de contrato) también pasa a "En progreso".
        if (requerimiento.getSolicitud() != null) {
            sincronizarSolicitudOrigenConOrden(requerimiento.getSolicitud(), orden, actor);
        }
        return ordenMapper.toResponse(orden);
    }

    @Override
    public RequerimientoResponse subirAdjunto(UUID id, MultipartFile file) {
        Requerimiento requerimiento = requerimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found"));
        autorizacion.exigirVisible(usuarioActual.obtener(), requerimiento);
        String referenciaAnterior = requerimiento.getUrl_adjunto();
        String nuevaReferencia = fileStorageService.store(file, TipoRecursoArchivo.REQUERIMIENTOS, id);
        requerimiento.setUrl_adjunto(nuevaReferencia);
        Requerimiento guardado = requerimientoRepository.save(requerimiento);
        if (referenciaAnterior != null) {
            fileStorageService.delete(referenciaAnterior);
        }
        return convertToResponse(guardado);
    }

    @Override
    public String obtenerReferenciaAdjunto(UUID id) {
        Requerimiento requerimiento = requerimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found"));
        autorizacion.exigirVisible(usuarioActual.obtener(), requerimiento);
        if (requerimiento.getUrl_adjunto() == null) {
            throw new ResourceNotFoundException("El Requerimiento no tiene adjunto.");
        }
        return requerimiento.getUrl_adjunto();
    }

    @Override
    public RequerimientoResponse eliminarAdjunto(UUID id) {
        Requerimiento requerimiento = requerimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found"));
        autorizacion.exigirVisible(usuarioActual.obtener(), requerimiento);
        if (requerimiento.getUrl_adjunto() != null) {
            fileStorageService.delete(requerimiento.getUrl_adjunto());
            requerimiento.setUrl_adjunto(null);
            requerimientoRepository.save(requerimiento);
        }
        return convertToResponse(requerimiento);
    }

    private void sincronizarSolicitudOrigenConOrden(Solicitud solicitud, Orden ordenGuardada, Usuario actor) {
        if (!"En revisión".equalsIgnoreCase(solicitud.getEstado().getNombre())) {
            // Ya no está "En revisión" (p. ej. ya se sincronizó antes o el
            // dato es legado): no reintentar la transición para no violar
            // SolicitudServiceImpl.validarTransicion ni pisar un estado más
            // avanzado (En progreso/Finalizado).
            return;
        }

        Estado estadoAnteriorSolicitud = solicitud.getEstado();
        Estado estadoEnProgresoSolicitud = estadoRepository.findByNombre("En progreso")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Estado 'En progreso' no está configurado en el catálogo."));

        solicitud.setEstado(estadoEnProgresoSolicitud);
        solicitudRepository.save(solicitud);

        HistorialSolicitud historialSolicitud = new HistorialSolicitud();
        historialSolicitud.setSolicitud(solicitud);
        historialSolicitud.setUsuario(actor);
        historialSolicitud.setEstado_anterior(estadoAnteriorSolicitud);
        historialSolicitud.setEstado_nuevo(estadoEnProgresoSolicitud);
        historialSolicitud.setFecha(Instant.now());
        historialSolicitud.setComentario("Orden de trabajo " + ordenGuardada.getNumeroOrden()
                + " generada desde el Requerimiento asociado; Solicitud pasa a En progreso.");
        historialSolicitudRepository.save(historialSolicitud);
    }

    private void validarTransicion(Estado actual, Estado nuevo) {
        if (actual.getId_estado().equals(nuevo.getId_estado())) {
            return;
        }
        Set<String> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(actual.getNombre().toLowerCase(), Set.of());
        if (!permitidos.contains(nuevo.getNombre().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transición de estado no permitida para Requerimiento: "
                            + actual.getNombre() + " -> " + nuevo.getNombre());
        }
    }

    private Usuario usuarioAutenticado() {
        return usuarioActual.obtener();
    }

    private List<Requerimiento> requerimientosVisibles() {
        return autorizacion.veTodosLosRequerimientos()
                ? requerimientoRepository.findAll()
                : requerimientoRepository.findVisiblesPara(usuarioActual.obtener().getId_usuario());
    }

    private RequerimientoResponse convertToResponse(Requerimiento requerimiento) {
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
