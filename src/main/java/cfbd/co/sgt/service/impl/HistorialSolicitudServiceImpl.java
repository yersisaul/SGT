package cfbd.co.sgt.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cfbd.co.sgt.dto.request.HistorialSolicitudRequest;
import cfbd.co.sgt.dto.response.HistorialSolicitudResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.HistorialSolicitud;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.HistorialSolicitudRepository;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.model.Solicitud;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.AutorizacionRecursoService;
import cfbd.co.sgt.service.HistorialSolicitudService;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class HistorialSolicitudServiceImpl implements HistorialSolicitudService {

    @Autowired
    private HistorialSolicitudRepository historialSolicitudRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioActualProvider usuarioActual;

    @Autowired
    private AutorizacionRecursoService autorizacion;

    @Override
    public HistorialSolicitudResponse crearHistorial(HistorialSolicitudRequest historialDTO) {
        HistorialSolicitud historial = new HistorialSolicitud();
        historial.setSolicitud(solicitudRepository.findById(historialDTO.getId_solicitud())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found")));
        historial.setEstado_anterior(estadoRepository.findById(historialDTO.getId_estado_anterior())
                .orElseThrow(() -> new ResourceNotFoundException("Estado anterior not found")));
        historial.setEstado_nuevo(estadoRepository.findById(historialDTO.getId_estado_nuevo())
                .orElseThrow(() -> new ResourceNotFoundException("Estado nuevo not found")));
        // El usuario que registra el cambio se obtiene del contexto de seguridad,
        // nunca de un id enviado por el cliente (CLAUDE.md 14.1).
        historial.setUsuario(usuarioAutenticado());
        historial.setComentario(historialDTO.getComentario());
        historial.setFecha(Instant.now());
        return convertToResponse(historialSolicitudRepository.save(historial));
    }

    @Override
    public List<HistorialSolicitudResponse> listarHistoriales(UUID idPadre) {
        // Visibilidad heredada del registro padre (CLAUDE.md 6.5): antes se
        // devolvía todo el historial a cualquiera con historial_solicitud.read.
        Usuario actor = usuarioActual.obtener();
        List<HistorialSolicitud> historiales;
        if (idPadre != null) {
            Solicitud padre = solicitudRepository.findById(idPadre)
                    .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found"));
            autorizacion.exigirVisible(actor, padre);
            historiales = historialSolicitudRepository.findByPadre(idPadre);
        } else if (autorizacion.veTodasLasSolicitudes()) {
            historiales = historialSolicitudRepository.findAll();
        } else {
            List<UUID> visibles = solicitudRepository.findVisiblesPara(actor.getId_usuario()).stream()
                    .map(Solicitud::getId_solicitud)
                    .toList();
            historiales = visibles.isEmpty() ? List.of() : historialSolicitudRepository.findByPadres(visibles);
        }
        return historiales.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HistorialSolicitudResponse> buscarHistorialPorId(UUID id) {
        Usuario actor = usuarioActual.obtener();
        return historialSolicitudRepository.findById(id)
                .filter(historial -> autorizacion.puedeVer(actor, historial.getSolicitud()))
                .map(this::convertToResponse);
    }

    private Usuario usuarioAutenticado() {
        return usuarioActual.obtener();
    }

    private HistorialSolicitudResponse convertToResponse(HistorialSolicitud historial) {
        HistorialSolicitudResponse response = new HistorialSolicitudResponse();
        response.setId_historial_solicitud(historial.getId_historial_solicitud());
        response.setId_solicitud(historial.getSolicitud().getId_solicitud());
        response.setId_usuario(historial.getUsuario().getId_usuario());
        response.setNombre_usuario((historial.getUsuario().getNombres() + " "
                + historial.getUsuario().getApellidos()).trim());
        response.setId_estado_anterior(historial.getEstado_anterior().getId_estado());
        response.setId_estado_nuevo(historial.getEstado_nuevo().getId_estado());
        response.setFecha(historial.getFecha());
        response.setComentario(historial.getComentario());
        return response;
    }
}
