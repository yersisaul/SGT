package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cfbd.co.sgt.dto.request.EstadoRequest;
import cfbd.co.sgt.dto.response.EstadoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Estado;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.service.EstadoService;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class EstadoServiceImpl implements EstadoService {

    @Autowired
    private EstadoRepository estadoRepository;

    @Override
    public EstadoResponse crearEstado(EstadoRequest estadoDTO) {
        Estado estado = new Estado();
        estado.setNombre(estadoDTO.getNombre());
        return convertToResponse(estadoRepository.save(estado));
    }

    @Override
    public EstadoResponse editarEstado(EstadoRequest estadoDTO, UUID id) {
        Estado estado = estadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado not found"));
        estado.setNombre(estadoDTO.getNombre());
        return convertToResponse(estadoRepository.save(estado));
    }

    @Override
    public List<EstadoResponse> listarEstados() {
        return estadoRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EstadoResponse> buscarEstadoPorId(UUID id) {
        return estadoRepository.findById(id).map(this::convertToResponse);
    }

    @Override
    public void eliminarEstado(UUID id) {
        estadoRepository.deleteById(id);
    }

    private EstadoResponse convertToResponse(Estado estado) {
        EstadoResponse response = new EstadoResponse();
        response.setId_estado(estado.getId_estado());
        response.setNombre(estado.getNombre());
        return response;
    }
}
