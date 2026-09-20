package cfbd.co.sgt.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public List<HistorialSolicitudResponse> listarHistoriales() {
        return historialSolicitudRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HistorialSolicitudResponse> buscarHistorialPorId(UUID id) {
        return historialSolicitudRepository.findById(id).map(this::convertToResponse);
    }

    private Usuario usuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private HistorialSolicitudResponse convertToResponse(HistorialSolicitud historial) {
        HistorialSolicitudResponse response = new HistorialSolicitudResponse();
        response.setId_historial_solicitud(historial.getId_historial_solicitud());
        response.setId_solicitud(historial.getSolicitud().getId_solicitud());
        response.setId_usuario(historial.getUsuario().getId_usuario());
        response.setId_estado_anterior(historial.getEstado_anterior().getId_estado());
        response.setId_estado_nuevo(historial.getEstado_nuevo().getId_estado());
        response.setFecha(historial.getFecha());
        response.setComentario(historial.getComentario());
        return response;
    }
}
