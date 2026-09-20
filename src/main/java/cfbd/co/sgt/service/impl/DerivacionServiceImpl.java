package cfbd.co.sgt.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import cfbd.co.sgt.dto.request.DerivacionRequest;
import cfbd.co.sgt.dto.response.DerivacionResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Derivacion;
import cfbd.co.sgt.model.Usuario;
import cfbd.co.sgt.repository.DerivacionRepository;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.service.DerivacionService;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class DerivacionServiceImpl implements DerivacionService {

    @Autowired
    private DerivacionRepository derivacionRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public DerivacionResponse crearDerivacion(DerivacionRequest derivacionDTO) {
        Derivacion derivacion = new Derivacion();
        derivacion.setSolicitud(solicitudRepository.findById(derivacionDTO.getId_solicitud())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud not found")));
        // El usuario origen (quien deriva) se obtiene del contexto de seguridad,
        // nunca de un id enviado por el cliente (CLAUDE.md 14.1).
        derivacion.setUsuario_origen(usuarioAutenticado());
        derivacion.setUsuario_destino(usuarioRepository.findById(derivacionDTO.getId_usuario_destino())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario destino not found")));
        derivacion.setMotivo(derivacionDTO.getMotivo());
        derivacion.setObservacion(derivacionDTO.getObservacion());
        derivacion.setFecha_derivacion(Instant.now());
        return convertToResponse(derivacionRepository.save(derivacion));
    }

    @Override
    public List<DerivacionResponse> listarDerivaciones() {
        return derivacionRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DerivacionResponse> buscarDerivacionPorId(UUID id) {
        return derivacionRepository.findById(id).map(this::convertToResponse);
    }

    private Usuario usuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));
    }

    private DerivacionResponse convertToResponse(Derivacion derivacion) {
        DerivacionResponse response = new DerivacionResponse();
        response.setId_derivacion(derivacion.getId_derivacion());
        response.setId_solicitud(derivacion.getSolicitud().getId_solicitud());
        response.setId_usuario_origen(derivacion.getUsuario_origen().getId_usuario());
        response.setId_usuario_destino(derivacion.getUsuario_destino().getId_usuario());
        response.setFecha_derivacion(derivacion.getFecha_derivacion());
        response.setMotivo(derivacion.getMotivo());
        response.setObservacion(derivacion.getObservacion());
        return response;
    }
}
