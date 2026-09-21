package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import jakarta.transaction.Transactional;
import cfbd.co.sgt.service.OrdenService;
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
import cfbd.co.sgt.dto.request.ReasignarOrdenRequest;
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
    // campo más (CLAUDE.md 5.3 — el PUT genérico no debe permitir saltarse
    // el flujo de cierre).
    private static final Set<String> ESTADOS_EJECUCION = Set.of("pendiente", "en progreso");

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

    @Override
    public OrdenResponse crearOrden(OrdenRequest ordenDTO) {
        Orden orden = new Orden();
        // El usuario se obtiene del contexto de seguridad, nunca del body
        // (CLAUDE.md 6.1/5.1). Nota: ningún rol tiene hoy orden.create — la
        // creación real ocurre vía generarOrdenDesdeSolicitud/Requerimiento;
        // este método queda disponible por si se habilita en el futuro.
        orden.setUsuario(usuarioAutenticado());
        orden.setEstado(estadoRepository.findById(ordenDTO.getId_estado()).orElse(null));
        orden.setEspecialidad(especialidadRepository.findById(ordenDTO.getId_especialidad()).orElse(null));
        orden.setSolicitud(ordenDTO.getId_solicitud() != null
                ? solicitudRepository.findById(ordenDTO.getId_solicitud()).orElse(null) : null);
        orden.setRequerimiento(ordenDTO.getId_requerimiento() != null
                ? requerimientoRepository.findById(ordenDTO.getId_requerimiento()).orElse(null) : null);
        orden.setUrl_adjunto(ordenDTO.getUrl_adjunto());
        Long correlativo = ordenRepository.count() + 1;
        orden.setNumeroOrden("OT-"+Long.toString(correlativo));
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
        if (esOperaciones(actor) && !orden.getUsuario().getId_usuario().equals(actor.getId_usuario())) {
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
        orden.setUrl_adjunto(ordenDTO.getUrl_adjunto());
        return convertToResponse(ordenRepository.save(orden));
    }

    @Override
    public List<OrdenResponse> listarOrdenes() {
        Usuario actor = usuarioAutenticado();
        List<Orden> ordenes = esOperaciones(actor)
                ? ordenRepository.findByUsuario(actor.getId_usuario())
                : ordenRepository.findAll();
        return ordenes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<OrdenResponse> buscarOrdenPorId(UUID id) {
        Usuario actor = usuarioAutenticado();
        return ordenRepository.findById(id)
                .filter(orden -> puedeVer(actor, orden))
                .map(this::convertToResponse);
    }

    @Override
    public Optional<OrdenResponse> buscarOrdenPorNumero(String numeroOrden) {
        Usuario actor = usuarioAutenticado();
        return ordenRepository.findByNumeroOrden(numeroOrden)
                .filter(orden -> puedeVer(actor, orden))
                .map(this::convertToResponse);
    }

    @Override
    public void eliminarOrden(UUID id) {
        ordenRepository.deleteById(id);
    }

    @Override
    public OrdenResponse cerrarOrden(UUID id, CerrarOrdenRequest request) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found"));

        if (orden.getFecha_cierre() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La Orden ya está cerrada.");
        }

        Usuario actor = usuarioAutenticado();
        if (esOperaciones(actor) && !orden.getUsuario().getId_usuario().equals(actor.getId_usuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puede cerrar una Orden que no tiene asignada.");
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

        // Trazabilidad Solicitud/Requerimiento -> OT (CLAUDE.md, flujo de
        // negocio): cerrar la OT es lo que finaliza la atención de la
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
    public OrdenResponse reasignarOrden(UUID id, ReasignarOrdenRequest request) {
        Orden orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found"));

        if (orden.getFecha_cierre() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede reasignar una Orden ya cerrada.");
        }

        Usuario actor = usuarioAutenticado();
        boolean esEjecutorActual = orden.getUsuario().getId_usuario().equals(actor.getId_usuario());
        boolean esAdministrador = "Administrador".equalsIgnoreCase(actor.getRol().getNombre());
        if (!esEjecutorActual && !esAdministrador) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el ejecutor actual o un Administrador pueden reasignar la Orden.");
        }

        Usuario nuevoEjecutor = usuarioRepository.findById(request.getId_usuario_nuevo())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario ejecutor no encontrado"));
        if (!"Operaciones".equalsIgnoreCase(nuevoEjecutor.getRol().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nuevo ejecutor debe tener rol Operaciones.");
        }

        Usuario ejecutorAnterior = orden.getUsuario();
        if (ejecutorAnterior.getId_usuario().equals(nuevoEjecutor.getId_usuario())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La Orden ya está asignada a ese usuario.");
        }

        orden.setUsuario(nuevoEjecutor);
        Orden ordenReasignada = ordenRepository.save(orden);

        // HistorialOrden no tiene campos usuario_anterior/usuario_nuevo; se
        // documenta la reasignación como una transición de estado "no-op"
        // (mismo patrón ya usado en RequerimientoServiceImpl al generar OT
        // desde un Requerimiento aprobado), dejando el detalle en comentario.
        HistorialOrden historial = new HistorialOrden();
        historial.setOrden(ordenReasignada);
        historial.setUsuario(actor);
        historial.setEstado_anterior(ordenReasignada.getEstado());
        historial.setEstado_nuevo(ordenReasignada.getEstado());
        historial.setFecha(Instant.now());
        historial.setComentario((request.getComentario() != null ? request.getComentario() + " — " : "")
                + "Reasignación: " + ejecutorAnterior.getNombres() + " " + ejecutorAnterior.getApellidos()
                + " → " + nuevoEjecutor.getNombres() + " " + nuevoEjecutor.getApellidos() + ".");
        historialOrdenRepository.save(historial);

        return convertToResponse(ordenReasignada);
    }

    private void finalizarSolicitudAsociada(Orden ordenCerrada, Usuario actor) {
        Solicitud solicitud = ordenCerrada.getSolicitud();
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
        historialSolicitud.setComentario(
                "Orden de trabajo " + ordenCerrada.getNumeroOrden() + " cerrada; Solicitud finalizada.");
        historialSolicitudRepository.save(historialSolicitud);
    }

    private void finalizarRequerimientoAsociado(Orden ordenCerrada, Usuario actor) {
        Requerimiento requerimiento = ordenCerrada.getRequerimiento();
        if ("Finalizado".equalsIgnoreCase(requerimiento.getEstado().getNombre())) {
            return;
        }

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

    private boolean esOperaciones(Usuario usuario) {
        return "Operaciones".equalsIgnoreCase(usuario.getRol().getNombre());
    }

    private boolean puedeVer(Usuario actor, Orden orden) {
        return !esOperaciones(actor) || orden.getUsuario().getId_usuario().equals(actor.getId_usuario());
    }

    private void validarTransicion(Estado actual, Estado nuevo) {
        if (actual.getId_estado().equals(nuevo.getId_estado())) {
            return;
        }
        String actualNombre = actual.getNombre().toLowerCase();
        String nuevoNombre = nuevo.getNombre().toLowerCase();
        if (!ESTADOS_EJECUCION.contains(actualNombre) || !ESTADOS_EJECUCION.contains(nuevoNombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transición de estado no permitida para Orden: " + actual.getNombre() + " -> " + nuevo.getNombre()
                            + ". El cierre debe hacerse mediante la operación de cierre dedicada.");
        }
    }

    private Usuario usuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private OrdenResponse convertToResponse(Orden orden){
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
