package cfbd.co.sgt.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import cfbd.co.sgt.dto.request.HistorialOrdenRequest;
import cfbd.co.sgt.dto.response.HistorialOrdenResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.HistorialOrden;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.HistorialOrdenRepository;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.service.HistorialOrdenService;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class HistorialOrdenServiceImpl implements HistorialOrdenService {

    @Autowired
    private HistorialOrdenRepository historialOrdenRepository;

    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public HistorialOrdenResponse crearHistorial(HistorialOrdenRequest historialDTO) {
        HistorialOrden historial = new HistorialOrden();
        historial.setOrden(ordenRepository.findById(historialDTO.getId_orden())
                .orElseThrow(() -> new ResourceNotFoundException("Orden not found")));
        historial.setEstado_anterior(estadoRepository.findById(historialDTO.getId_estado_anterior())
                .orElseThrow(() -> new ResourceNotFoundException("Estado anterior not found")));
        historial.setEstado_nuevo(estadoRepository.findById(historialDTO.getId_estado_nuevo())
                .orElseThrow(() -> new ResourceNotFoundException("Estado nuevo not found")));
        // El usuario que registra el cambio se obtiene del contexto de seguridad,
        // nunca de un id enviado por el cliente (CLAUDE.md 14.1).
        historial.setUsuario(usuarioAutenticado());
        historial.setComentario(historialDTO.getComentario());
        historial.setFecha(Instant.now());
        return convertToResponse(historialOrdenRepository.save(historial));
    }

    @Override
    public List<HistorialOrdenResponse> listarHistoriales() {
        return historialOrdenRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HistorialOrdenResponse> buscarHistorialPorId(UUID id) {
        return historialOrdenRepository.findById(id).map(this::convertToResponse);
    }

    private Usuario usuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private HistorialOrdenResponse convertToResponse(HistorialOrden historial) {
        HistorialOrdenResponse response = new HistorialOrdenResponse();
        response.setId_historial_orden(historial.getId_historial_orden());
        response.setId_orden(historial.getOrden().getId_orden());
        response.setId_usuario(historial.getUsuario().getId_usuario());
        response.setId_estado_anterior(historial.getEstado_anterior().getId_estado());
        response.setId_estado_nuevo(historial.getEstado_nuevo().getId_estado());
        response.setFecha(historial.getFecha());
        response.setComentario(historial.getComentario());
        return response;
    }
}
