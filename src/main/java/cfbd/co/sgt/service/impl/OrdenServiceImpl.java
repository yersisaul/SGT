package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import jakarta.transaction.Transactional;
import cfbd.co.sgt.mapper.OrdenMapper;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.AutorizacionRecursoService;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.NumeracionService;
import cfbd.co.sgt.service.OrdenService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.repository.RequerimientoRepository;
import cfbd.co.sgt.repository.HistorialOrdenRepository;
import cfbd.co.sgt.repository.HistorialRequerimientoRepository;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.HistorialOrden;
import cfbd.co.sgt.model.HistorialRequerimiento;
import cfbd.co.sgt.model.HistorialSolicitud;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.dto.request.CerrarOrdenRequest;
import cfbd.co.sgt.dto.request.OrdenRequest;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.repository.HistorialSolicitudRepository;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.Instant;

@Service
@Transactional
public class OrdenServiceImpl implements OrdenService {

    // Estados de ejecución entre los que el PUT genérico puede mover una
    // Orden. Orden solo maneja Pendiente/En progreso/Finalizado (sin "En
    // revisión"). "Finalizado" está excluido a propósito: cerrar una Orden es
    // una operación de negocio específica (ver cerrarOrden), no un cambio de
    // campo más.
    // El PUT genérico ya no cambia el estado de la OT: la cola (tomar,
    // asignar, verificar, reasignar) y el cierre son operaciones de negocio
    // dedicadas. Se conserva solo el no-op.

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private RequerimientoRepository requerimientoRepository;

    @Autowired
    private HistorialOrdenRepository historialOrdenRepository;

    @Autowired
    private HistorialSolicitudRepository historialSolicitudRepository;

    @Autowired
    private HistorialRequerimientoRepository historialRequerimientoRepository;

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private AutorizacionRecursoService autorizacion;
    @Autowired
    private NumeracionService numeracion;
    @Autowired
    private UsuarioActualProvider usuarioActual;
    @Autowired
    private OrdenMapper ordenMapper;

    @Override
    public OrdenResponse crearOrden(OrdenRequest ordenDTO) {
        Orden orden = new Orden();
        // El usuario se obtiene del contexto de seguridad, nunca del body
        // Nota: ningún rol tiene hoy orden.create — la
        // creación real ocurre vía generarOrdenDesdeSolicitud/Requerimiento;
        // este método queda disponible por si se habilita en el futuro.
        orden.setUsuario(usuarioAutenticado());
        orden.setEstado(estadoRepository.findById(ordenDTO.getId_estado())
                .orElseThrow(() -> new ResourceNotFoundException("Estado not found")));
        orden.setEspecialidad(especialidadRepository.findById(ordenDTO.getId_especialidad())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found")));
        orden.setSolicitud(ordenDTO.getId_solicitud() != null
                ? solicitudRepository.findById(ordenDTO.getId_solicitud())
                        .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found"))
                : null);
        orden.setRequerimiento(ordenDTO.getId_requerimiento() != null
                ? requerimientoRepository.findById(ordenDTO.getId_requerimiento())
                        .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found"))
                : null);
        // El adjunto se gestiona exclusivamente vía subirAdjunto/eliminarAdjunto no se acepta desde este DTO.
        orden.setNumeroOrden(numeracion.siguienteNumeroOrden());
        orden.setFecha_registro(Instant.now());
        return convertToResponse(ordenRepository.save(orden));
    }

    @Override
    public OrdenResponse editarOrden(OrdenRequest ordenDTO, UUID id) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found"));

        if (orden.getFecha_cierre() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La Orden ya está cerrada.");
        }

        Usuario actor = usuarioAutenticado();
        autorizacion.exigirVisible(actor, orden);
        if (!esEjecutor(actor, orden) && !autorizacion.veTodasLasOrdenes()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede modificar una Orden que no tiene asignada.");
        }

        Estado estadoNuevo = estadoRepository.findById(ordenDTO.getId_estado())
                .orElseThrow(() -> new ResourceNotFoundException("Estado not found"));
        validarTransicion(orden.getEstado(), estadoNuevo);

        // id_usuario, id_especialidad, id_solicitud e id_requerimiento son
        // hechos fijados al generar la Orden; este PUT genérico no los
        // reasigna (evita que se pueda "reenlazar" una OT a otro origen o
        // ejecutor — para eso existe reasignarOrden).
        orden.setEstado(estadoNuevo);
        return convertToResponse(ordenRepository.save(orden));
    }

    @Override
    public List<OrdenResponse> listarOrdenes() {
        List<Orden> ordenes = autorizacion.veTodasLasOrdenes()
                ? ordenRepository.findAll()
                : ordenRepository.findVisiblesPara(usuarioAutenticado().getId_usuario());
        return ordenes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<OrdenResponse> buscarOrdenPorId(UUID id) {
        Usuario actor = usuarioAutenticado();
        return ordenRepository.findById(id)
                .filter(orden -> autorizacion.puedeVer(actor, orden))
                .map(this::convertToResponse);
    }

    @Override
    public Optional<OrdenResponse> buscarOrdenPorNumero(String numeroOrden) {
        Usuario actor = usuarioAutenticado();
        return ordenRepository.findByNumeroOrden(numeroOrden)
                .filter(orden -> autorizacion.puedeVer(actor, orden))
                .map(this::convertToResponse);
    }

    @Override
    public void eliminarOrden(UUID id) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found"));
        // Toda OT generada por el flujo tiene historial: es auditoría, no se borra.
        if (historialOrdenRepository.existsByPadre(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar una Orden con historial.");
        }
        ordenRepository.delete(orden);
    }

    @Override
    public OrdenResponse cerrarOrden(UUID id, CerrarOrdenRequest request) {
        Orden orden = ordenRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found"));

        if (orden.getFecha_cierre() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La Orden ya está cerrada.");
        }

        Usuario actor = usuarioAutenticado();
        autorizacion.exigirVisible(actor, orden);
        if (!esEjecutor(actor, orden)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el ejecutor asignado puede cerrar la Orden.");
        }
        // Pasos 11-14: solo se cierra lo que el ejecutor confirmó y ejecutó.
        if (!"En progreso".equalsIgnoreCase(orden.getEstado().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede cerrar una Orden 'En progreso' (actual: " + orden.getEstado().getNombre() + ").");
        }

        Estado estadoFinalizado = estadoRepository.findByNombre("Finalizado")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Estado 'Finalizado' no está configurado en el catálogo."));
        Estado estadoAnterior = orden.getEstado();

        orden.setEstado(estadoFinalizado);
        orden.setFecha_cierre(Instant.now());
        Orden ordenCerrada = ordenRepository.save(orden);

        HistorialOrden historial = new HistorialOrden();
        historial.setOrden(ordenCerrada);
        historial.setUsuario(actor);
        historial.setEstado_anterior(estadoAnterior);
        historial.setEstado_nuevo(estadoFinalizado);
        historial.setFecha(Instant.now());
        historial.setComentario(request != null && request.getComentario() != null
                ? request.getComentario()
                : "Orden cerrada.");
        historialOrdenRepository.save(historial);

        // Trazabilidad Solicitud/Requerimiento -> OT cerrar la OT es lo que finaliza la atención de la
        // Solicitud o el Requerimiento que la originó.
        if (ordenCerrada.getSolicitud() != null) {
            finalizarSolicitudAsociada(ordenCerrada, actor);
        }
        if (ordenCerrada.getRequerimiento() != null) {
            finalizarRequerimientoAsociado(ordenCerrada, actor);
        }

        return convertToResponse(ordenCerrada);
    }

    @Override
    public OrdenResponse subirAdjunto(UUID id, MultipartFile file) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found"));
        if (!autorizacion.puedeVer(usuarioAutenticado(), orden)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta Orden.");
        }
        String referenciaAnterior = orden.getUrl_adjunto();
        String nuevaReferencia = fileStorageService.store(file, TipoRecursoArchivo.ORDENES, id);
        orden.setUrl_adjunto(nuevaReferencia);
        Orden guardada = ordenRepository.save(orden);
        if (referenciaAnterior != null) {
            fileStorageService.delete(referenciaAnterior);
        }
        return convertToResponse(guardada);
    }

    @Override
    public String obtenerReferenciaAdjunto(UUID id) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found"));
        if (!autorizacion.puedeVer(usuarioAutenticado(), orden)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta Orden.");
        }
        if (orden.getUrl_adjunto() == null) {
            throw new ResourceNotFoundException("La Orden no tiene adjunto.");
        }
        return orden.getUrl_adjunto();
    }

    @Override
    public OrdenResponse eliminarAdjunto(UUID id) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found"));
        if (!autorizacion.puedeVer(usuarioAutenticado(), orden)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a esta Orden.");
        }
        if (orden.getUrl_adjunto() != null) {
            fileStorageService.delete(orden.getUrl_adjunto());
            orden.setUrl_adjunto(null);
            ordenRepository.save(orden);
        }
        return convertToResponse(orden);
    }

    private void finalizarSolicitudAsociada(Orden ordenCerrada, Usuario actor) {
        finalizarSolicitud(ordenCerrada.getSolicitud(), actor,
                "Orden de trabajo " + ordenCerrada.getNumeroOrden() + " cerrada; Solicitud finalizada.");
    }

    private void finalizarRequerimientoAsociado(Orden ordenCerrada, Usuario actor) {
        Requerimiento requerimiento = ordenCerrada.getRequerimiento();
        if (!"Finalizado".equalsIgnoreCase(requerimiento.getEstado().getNombre())) {
            Estado estadoAnteriorRequerimiento = requerimiento.getEstado();
            Estado estadoFinalizado = estadoRepository.findByNombre("Finalizado")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Estado 'Finalizado' no está configurado en el catálogo."));

            requerimiento.setEstado(estadoFinalizado);
            requerimientoRepository.save(requerimiento);

            HistorialRequerimiento historialRequerimiento = new HistorialRequerimiento();
            historialRequerimiento.setRequerimiento(requerimiento);
            historialRequerimiento.setUsuario(actor);
            historialRequerimiento.setEstado_anterior(estadoAnteriorRequerimiento);
            historialRequerimiento.setEstado_nuevo(estadoFinalizado);
            historialRequerimiento.setFecha(Instant.now());
            historialRequerimiento.setComentario(
                    "Orden de trabajo " + ordenCerrada.getNumeroOrden() + " cerrada; Requerimiento finalizado.");
            historialRequerimientoRepository.save(historialRequerimiento);
        }

        // Cascada Solicitud -> Requerimiento -> OT: si este Requerimiento se
        // generó desde una Solicitud fuera de contrato (Requerimiento.solicitud),
        // cerrar la OT también finaliza esa Solicitud — mismo criterio que el
        // camino directo Solicitud -> OT (finalizarSolicitudAsociada).
        if (requerimiento.getSolicitud() != null) {
            finalizarSolicitud(requerimiento.getSolicitud(), actor,
                    "Orden de trabajo " + ordenCerrada.getNumeroOrden() + " (vía Requerimiento "
                            + requerimiento.getNumeroRequerimiento() + ") cerrada; Solicitud finalizada.");
        }
    }

    private void finalizarSolicitud(Solicitud solicitud, Usuario actor, String comentario) {
        if ("Finalizado".equalsIgnoreCase(solicitud.getEstado().getNombre())) {
            return;
        }

        Estado estadoAnteriorSolicitud = solicitud.getEstado();
        Estado estadoFinalizado = estadoRepository.findByNombre("Finalizado")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Estado 'Finalizado' no está configurado en el catálogo."));

        solicitud.setEstado(estadoFinalizado);
        solicitudRepository.save(solicitud);

        HistorialSolicitud historialSolicitud = new HistorialSolicitud();
        historialSolicitud.setSolicitud(solicitud);
        historialSolicitud.setUsuario(actor);
        historialSolicitud.setEstado_anterior(estadoAnteriorSolicitud);
        historialSolicitud.setEstado_nuevo(estadoFinalizado);
        historialSolicitud.setFecha(Instant.now());
        historialSolicitud.setComentario(comentario);
        historialSolicitudRepository.save(historialSolicitud);
    }

    private boolean esEjecutor(Usuario actor, Orden orden) {
        return orden.getUsuario() != null && orden.getUsuario().getId_usuario().equals(actor.getId_usuario());
    }

    private void validarTransicion(Estado actual, Estado nuevo) {
        if (actual.getId_estado().equals(nuevo.getId_estado())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Transición de estado no permitida para Orden: " + actual.getNombre() + " -> " + nuevo.getNombre()
                        + ". Use las operaciones tomar/asignar/verificar/reasignar/cerrar.");
    }

    private Usuario usuarioAutenticado() {
        return usuarioActual.obtener();
    }

    private OrdenResponse convertToResponse(Orden orden) {
        return ordenMapper.toResponse(orden);
    }
}
