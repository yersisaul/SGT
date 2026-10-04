package cfbd.co.sgt.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cfbd.co.sgt.dto.request.AprobacionRequest;
import cfbd.co.sgt.dto.response.AprobacionResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Aprobacion;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.HistorialRequerimiento;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.AprobacionRepository;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.HistorialRequerimientoRepository;
import cfbd.co.sgt.repository.RequerimientoRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.service.AprobacionService;
import cfbd.co.sgt.dto.request.GenerarOrdenRequest;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.service.EstadoResolver;
import cfbd.co.sgt.service.EstadosNegocio;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.RegistroHistorialService;
import cfbd.co.sgt.service.RequerimientoService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class AprobacionServiceImpl implements AprobacionService {

    @Autowired
    private AprobacionRepository aprobacionRepository;

    @Autowired
    private RequerimientoRepository requerimientoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private HistorialRequerimientoRepository historialRequerimientoRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private RequerimientoService requerimientoService;

    @Autowired
    private RegistroHistorialService registro;

    @Autowired
    private EstadoResolver estados;

    /**
     * Aprobar/rechazar un Requerimiento es una única operación de negocio
     * (requerimiento.aprobar en el Controller): el body sigue siendo el
     * mismo AprobacionRequest.aprobado booleano, pero además de registrar la
     * Aprobacion ahora transiciona el Requerimiento a "Aprobado"/"Rechazado"
     * y deja historial — todo en la misma transacción.
     */
    @Override
    public AprobacionResponse crearAprobacion(AprobacionRequest aprobacionDTO) {
        Requerimiento requerimiento = requerimientoRepository.findById(aprobacionDTO.getId_requerimiento())
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found"));

        String actualNombre = requerimiento.getEstado().getNombre().toLowerCase();
        if (!actualNombre.equals("pendiente") && !actualNombre.equals("en revisión")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede aprobar/rechazar un Requerimiento en estado 'Pendiente' o 'En revisión' (actual: "
                            + requerimiento.getEstado().getNombre() + ").");
        }

        boolean aprobado = Boolean.TRUE.equals(aprobacionDTO.getAprobado());
        String nombreEstadoDestino = aprobado ? "Aprobado" : "Rechazado";
        Estado estadoDestino = estadoRepository.findByNombre(nombreEstadoDestino)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Estado '" + nombreEstadoDestino + "' no está configurado en el catálogo."));

        Usuario actor = usuarioAutenticado();
        Estado estadoAnterior = requerimiento.getEstado();

        Aprobacion aprobacion = new Aprobacion();
        aprobacion.setRequerimiento(requerimiento);
        // El usuario aprobador se obtiene del contexto de seguridad, nunca de un id
        // enviado por el cliente (CLAUDE.md 6.1/5.1).
        aprobacion.setUsuario(actor);
        aprobacion.setAprobado(aprobacionDTO.getAprobado());
        aprobacion.setComentario(aprobacionDTO.getComentario());
        // El adjunto (presupuesto/documento) se gestiona exclusivamente vía
        // subirAdjunto/eliminarAdjunto (fileserver propio, CLAUDE.md sección
        // 26/30), después de creada la Aprobacion.
        aprobacion.setFecha_aprobacion(Instant.now());
        Aprobacion aprobacionGuardada = aprobacionRepository.save(aprobacion);

        requerimiento.setEstado(estadoDestino);
        requerimientoRepository.save(requerimiento);

        HistorialRequerimiento historial = new HistorialRequerimiento();
        historial.setRequerimiento(requerimiento);
        historial.setUsuario(actor);
        historial.setEstado_anterior(estadoAnterior);
        historial.setEstado_nuevo(estadoDestino);
        historial.setFecha(Instant.now());
        historial.setComentario(aprobacionDTO.getComentario());
        historialRequerimientoRepository.save(historial);

        Solicitud origen = requerimiento.getSolicitud();
        if (!aprobado && origen != null) {
            // Paso 8 "NO → FIN": la Solicitud del cliente se cierra como
            // "Rechazado" con el motivo visible en su seguimiento (PRD D6).
            Estado anteriorSolicitud = origen.getEstado();
            Estado rechazado = estados.porNombre(EstadosNegocio.RECHAZADO);
            origen.setEstado(rechazado);
            solicitudRepository.save(origen);
            registro.solicitud(origen, actor, anteriorSolicitud, rechazado,
                    "Requerimiento " + requerimiento.getNumeroRequerimiento() + " rechazado"
                            + (aprobacionDTO.getComentario() != null && !aprobacionDTO.getComentario().isBlank()
                                    ? ": " + aprobacionDTO.getComentario() : "."));
        }
        if (aprobado && aprobacionDTO.getId_especialidad_orden() != null) {
            // Paso 9 con el modal confirmado (PRD D7): la OT se genera en la
            // misma transacción; si algo falla, tampoco queda la aprobación.
            GenerarOrdenRequest generar = new GenerarOrdenRequest();
            generar.setId_especialidad(aprobacionDTO.getId_especialidad_orden());
            requerimientoService.generarOrdenDesdeRequerimiento(requerimiento.getId_requerimiento(), generar);
        }

        return convertToResponse(aprobacionGuardada);
    }

    @Override
    public List<AprobacionResponse> listarAprobaciones() {
        return aprobacionRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AprobacionResponse> buscarAprobacionPorId(UUID id) {
        return aprobacionRepository.findById(id).map(this::convertToResponse);
    }

    @Override
    public AprobacionResponse subirAdjunto(UUID id, MultipartFile file) {
        Aprobacion aprobacion = aprobacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aprobacion not found"));
        String referenciaAnterior = aprobacion.getUrl_adjunto();
        String nuevaReferencia = fileStorageService.store(file, TipoRecursoArchivo.APROBACIONES, id);
        aprobacion.setUrl_adjunto(nuevaReferencia);
        Aprobacion guardada = aprobacionRepository.save(aprobacion);
        if (referenciaAnterior != null) {
            fileStorageService.delete(referenciaAnterior);
        }
        return convertToResponse(guardada);
    }

    @Override
    public String obtenerReferenciaAdjunto(UUID id) {
        Aprobacion aprobacion = aprobacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aprobacion not found"));
        if (aprobacion.getUrl_adjunto() == null) {
            throw new ResourceNotFoundException("La Aprobacion no tiene adjunto.");
        }
        return aprobacion.getUrl_adjunto();
    }

    @Override
    public AprobacionResponse eliminarAdjunto(UUID id) {
        Aprobacion aprobacion = aprobacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aprobacion not found"));
        if (aprobacion.getUrl_adjunto() != null) {
            fileStorageService.delete(aprobacion.getUrl_adjunto());
            aprobacion.setUrl_adjunto(null);
            aprobacionRepository.save(aprobacion);
        }
        return convertToResponse(aprobacion);
    }

    private Usuario usuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private AprobacionResponse convertToResponse(Aprobacion aprobacion) {
        AprobacionResponse response = new AprobacionResponse();
        response.setId_aprobacion(aprobacion.getId_aprobacion());
        response.setId_requerimiento(aprobacion.getRequerimiento().getId_requerimiento());
        response.setId_usuario(aprobacion.getUsuario().getId_usuario());
        response.setAprobado(aprobacion.getAprobado());
        response.setComentario(aprobacion.getComentario());
        response.setFecha_aprobacion(aprobacion.getFecha_aprobacion());
        response.setUrl_adjunto(aprobacion.getUrl_adjunto() != null
                ? "/api/archivos/aprobaciones/" + aprobacion.getId_aprobacion() : null);
        return response;
    }
}
