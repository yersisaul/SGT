package cfbd.co.sgt.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import cfbd.co.sgt.dto.request.HistorialRequerimientoRequest;
import cfbd.co.sgt.dto.response.HistorialRequerimientoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.HistorialRequerimiento;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.HistorialRequerimientoRepository;
import cfbd.co.sgt.repository.RequerimientoRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.service.HistorialRequerimientoService;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class HistorialRequerimientoServiceImpl implements HistorialRequerimientoService {

    @Autowired
    private HistorialRequerimientoRepository historialRequerimientoRepository;

    @Autowired
    private RequerimientoRepository requerimientoRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public HistorialRequerimientoResponse crearHistorial(HistorialRequerimientoRequest historialDTO) {
        HistorialRequerimiento historial = new HistorialRequerimiento();
        historial.setRequerimiento(requerimientoRepository.findById(historialDTO.getId_requerimiento())
                .orElseThrow(() -> new ResourceNotFoundException("Requerimiento not found")));
        historial.setEstado_anterior(estadoRepository.findById(historialDTO.getId_estado_anterior())
                .orElseThrow(() -> new ResourceNotFoundException("Estado anterior not found")));
        historial.setEstado_nuevo(estadoRepository.findById(historialDTO.getId_estado_nuevo())
                .orElseThrow(() -> new ResourceNotFoundException("Estado nuevo not found")));
        // El usuario que registra el cambio se obtiene del contexto de seguridad,
        // nunca de un id enviado por el cliente (CLAUDE.md 14.1).
        historial.setUsuario(usuarioAutenticado());
        historial.setComentario(historialDTO.getComentario());
        historial.setFecha(Instant.now());
        return convertToResponse(historialRequerimientoRepository.save(historial));
    }

    @Override
    public List<HistorialRequerimientoResponse> listarHistoriales() {
        return historialRequerimientoRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<HistorialRequerimientoResponse> buscarHistorialPorId(UUID id) {
        return historialRequerimientoRepository.findById(id).map(this::convertToResponse);
    }

    private Usuario usuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private HistorialRequerimientoResponse convertToResponse(HistorialRequerimiento historial) {
        HistorialRequerimientoResponse response = new HistorialRequerimientoResponse();
        response.setId_historial_requerimiento(historial.getId_historial_requerimiento());
        response.setId_requerimiento(historial.getRequerimiento().getId_requerimiento());
        response.setId_usuario(historial.getUsuario().getId_usuario());
        response.setId_estado_anterior(historial.getEstado_anterior().getId_estado());
        response.setId_estado_nuevo(historial.getEstado_nuevo().getId_estado());
        response.setFecha(historial.getFecha());
        response.setComentario(historial.getComentario());
        return response;
    }
}
