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
import cfbd.co.sgt.dto.request.RequerimientoRequest;
import cfbd.co.sgt.dto.response.EstadoCantidadResponse;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.dto.response.RequerimientoResponse;
import cfbd.co.sgt.dto.response.ResumenEstadosResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.model.HistorialRequerimiento;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.model.Requerimiento;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.HistorialRequerimientoRepository;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.repository.RequerimientoRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.service.RequerimientoService;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class RequerimientoServiceImpl implements RequerimientoService {

    // Transiciones válidas vía PUT genérico. "Aprobado"/"Rechazado" están
    // excluidos deliberadamente: solo se alcanzan a través de
    // AprobacionServiceImpl.crearAprobacion (operación de negocio real), no
    // por edición libre (CLAUDE.md 5.3).
    private static final Map<String, Set<String>> TRANSICIONES_PERMITIDAS = Map.of(
            "pendiente", Set.of("en revisión"),
            "en revisión", Set.of("pendiente"),
            "aprobado", Set.of(),
            "rechazado", Set.of());

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

    @Override
    public RequerimientoResponse crearRequerimiento(RequerimientoRequest requerimientoDTO) {
        Requerimiento requerimiento = new Requerimiento();
        // El usuario se obtiene del contexto de seguridad, nunca del body.
        requerimiento.setUsuario(usuarioAutenticado());
        requerimiento.setEstado(estadoRepository.findById(requerimientoDTO.getId_estado())
                .orElseThrow(() -> new ResourceNotFoundException("Estado not found")));
        requerimiento.setEspecialidad(especialidadRepository.findById(requerimientoDTO.getId_especialidad())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found")));
        requerimiento.setDescripcion(requerimientoDTO.getDescripcion());
        requerimiento.setUrl_adjunto(requerimientoDTO.getUrl_adjunto());
        Long correlativo = requerimientoRepository.count() + 1;
        requerimiento.setNumeroRequerimiento("RQ-" + correlativo);
        requerimiento.setFecha_registro(Instant.now());
        return convertToResponse(requerimientoRepository.save(requerimiento));
    }

    @Override
    public RequerimientoResponse editarRequerimiento(RequerimientoRequest requerimientoDTO, UUID id) {
        Requerimiento requerimiento = requerimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found"));

        String actualNombre = requerimiento.getEstado().getNombre().toLowerCase();
        if (actualNombre.equals("aprobado") || actualNombre.equals("rechazado")) {
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
        requerimiento.setUrl_adjunto(requerimientoDTO.getUrl_adjunto());
        return convertToResponse(requerimientoRepository.save(requerimiento));
    }

    @Override
    public List<RequerimientoResponse> listarRequerimientos() {
        return requerimientoRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RequerimientoResponse> buscarRequerimientoPorId(UUID id) {
        return requerimientoRepository.findById(id).map(this::convertToResponse);
    }

    @Override
    public Optional<RequerimientoResponse> buscarRequerimientoPorNumero(String numeroRequerimiento) {
        return requerimientoRepository.findByNumeroRequerimiento(numeroRequerimiento).map(this::convertToResponse);
    }

    @Override
    public void eliminarRequerimiento(UUID id) {
        requerimientoRepository.deleteById(id);
    }

    @Override
    public ResumenEstadosResponse obtenerResumenPorEstado() {
        Map<String, Long> conteosPorEstado = new HashMap<>();
        for (Object[] fila : requerimientoRepository.countByEstado()) {
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
        response.setTotal(requerimientoRepository.count());
        response.setPorEstado(porEstado);
        return response;
    }

    @Override
    public OrdenResponse generarOrdenDesdeRequerimiento(UUID idRequerimiento, GenerarOrdenRequest request) {
        Requerimiento requerimiento = requerimientoRepository.findById(idRequerimiento)
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found"));

        if (!"Aprobado".equalsIgnoreCase(requerimiento.getEstado().getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede generar una Orden desde un Requerimiento en estado 'Aprobado' (actual: "
                            + requerimiento.getEstado().getNombre() + ").");
        }
        if (ordenRepository.existsByRequerimiento(idRequerimiento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una Orden generada para este Requerimiento.");
        }

        Estado estadoInicialOrden = estadoRepository.findByNombre("Pendiente")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Estado 'Pendiente' no está configurado en el catálogo."));
        Usuario actor = usuarioAutenticado();

        Orden orden = new Orden();
        orden.setUsuario(actor);
        orden.setEstado(estadoInicialOrden);
        orden.setEspecialidad(requerimiento.getEspecialidad());
        orden.setSolicitud(null);
        orden.setRequerimiento(requerimiento);
        orden.setUrl_adjunto(request != null ? request.getUrl_adjunto() : null);
        Long correlativo = ordenRepository.count() + 1;
        orden.setNumeroOrden("OT-" + correlativo);
        orden.setFecha_registro(Instant.now());
        Orden ordenGuardada = ordenRepository.save(orden);

        // El Requerimiento se queda en "Aprobado" (no hay estado "Finalizado"
        // definido para Requerimiento): el ciclo de vida posterior lo lleva
        // la Orden. El historial deja constancia de que se generó la OT.
        HistorialRequerimiento historial = new HistorialRequerimiento();
        historial.setRequerimiento(requerimiento);
        historial.setUsuario(actor);
        historial.setEstado_anterior(requerimiento.getEstado());
        historial.setEstado_nuevo(requerimiento.getEstado());
        historial.setFecha(Instant.now());
        historial.setComentario(request != null && request.getComentario() != null
                ? request.getComentario()
                : "Orden de trabajo " + ordenGuardada.getNumeroOrden() + " generada desde el Requerimiento aprobado.");
        historialRequerimientoRepository.save(historial);

        return convertirOrdenAResponse(ordenGuardada);
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
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private RequerimientoResponse convertToResponse(Requerimiento requerimiento) {
        RequerimientoResponse response = new RequerimientoResponse();
        response.setId_requerimiento(requerimiento.getId_requerimiento());
        response.setId_usuario(requerimiento.getUsuario().getId_usuario());
        response.setId_estado(requerimiento.getEstado().getId_estado());
        response.setId_especialidad(requerimiento.getEspecialidad().getId_especialidad());
        response.setNumeroRequerimiento(requerimiento.getNumeroRequerimiento());
        response.setFecha_registro(requerimiento.getFecha_registro());
        response.setDescripcion(requerimiento.getDescripcion());
        response.setUrl_adjunto(requerimiento.getUrl_adjunto());
        return response;
    }

    private OrdenResponse convertirOrdenAResponse(Orden orden) {
        OrdenResponse response = new OrdenResponse();
        response.setId_orden(orden.getId_orden());
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
