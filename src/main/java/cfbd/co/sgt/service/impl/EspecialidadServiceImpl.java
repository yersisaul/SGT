package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cfbd.co.sgt.dto.request.EspecialidadRequest;
import cfbd.co.sgt.dto.response.EspecialidadResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Especialidad;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.service.EspecialidadService;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class EspecialidadServiceImpl implements EspecialidadService {

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Override
    public EspecialidadResponse crearEspecialidad(EspecialidadRequest especialidadDTO) {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(especialidadDTO.getNombre());
        especialidad.setDescripcion(especialidadDTO.getDescripcion());
        return convertToResponse(especialidadRepository.save(especialidad));
    }

    @Override
    public EspecialidadResponse editarEspecialidad(EspecialidadRequest especialidadDTO, UUID id) {
        Especialidad especialidad = especialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found"));
        especialidad.setNombre(especialidadDTO.getNombre());
        especialidad.setDescripcion(especialidadDTO.getDescripcion());
        return convertToResponse(especialidadRepository.save(especialidad));
    }

    @Override
    public List<EspecialidadResponse> listarEspecialidades() {
        return especialidadRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EspecialidadResponse> buscarEspecialidadPorId(UUID id) {
        return especialidadRepository.findById(id).map(this::convertToResponse);
    }

    @Override
    public void eliminarEspecialidad(UUID id) {
        especialidadRepository.deleteById(id);
    }

    private EspecialidadResponse convertToResponse(Especialidad especialidad) {
        EspecialidadResponse response = new EspecialidadResponse();
        response.setId_especialidad(especialidad.getId_especialidad());
        response.setNombre(especialidad.getNombre());
        response.setDescripcion(especialidad.getDescripcion());
        return response;
    }
}
