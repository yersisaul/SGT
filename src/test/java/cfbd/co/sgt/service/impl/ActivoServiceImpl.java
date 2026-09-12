package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import cfbd.co.sgt.service.ActivoService;
import cfbd.co.sgt.repository.ActivoRepository;
import cfbd.co.sgt.dto.request.ActivoRequest;
import cfbd.co.sgt.dto.response.ActivoResponse;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.model.Activo;
import java.util.UUID;
import java.util.stream.Collectors;

@Service 
@Transactional 
public class ActivoServiceImpl implements ActivoService {
    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Override
    public Activo crearActivo(ActivoRequest activoDTO) {        
        Activo activo = new Activo();
        activo.setEspecialidad(especialidadRepository.findById(activoDTO.getId_especialidad()).orElse(null));
        activo.setCodigo(activoDTO.getCodigo());
        activo.setNombre(activoDTO.getNombre());
        activo.setDescripcion(activoDTO.getDescripcion());
        activo.setUbicacion(activoDTO.getUbicacion());
        activo.setUrl_img(activoDTO.getUrl_img());
        return activoRepository.save(activo);
    }

    @Override
    public Activo editarActivo(ActivoRequest activoDTO, UUID id) {
        Activo activo = activoRepository.findById(id).orElse(null);
        if (activo != null) {
            activo.setEspecialidad(especialidadRepository.findById(activoDTO.getId_especialidad()).orElse(null));
            activo.setCodigo(activoDTO.getCodigo());
            activo.setNombre(activoDTO.getNombre());
            activo.setDescripcion(activoDTO.getDescripcion());
            activo.setUbicacion(activoDTO.getUbicacion());
            activo.setUrl_img(activoDTO.getUrl_img());
        }
        return activoRepository.save(activo);
    }

    @Override
    public List<ActivoResponse> listarActivos() {
        List<Activo> activos = activoRepository.findAll();
        return activos.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ActivoResponse> buscarActivoPorId(UUID id) {
        return activoRepository.findById(id).map(this::convertToResponse);
    }

    @Override
    public void eliminarActivo(UUID id) {
        activoRepository.deleteById(id);
    }

    private ActivoResponse convertToResponse(Activo activo) {
        ActivoResponse response = new ActivoResponse();
        response.setId_activo(activo.getId_activo());
        response.setId_especialidad(activo.getEspecialidad().getId_especialidad());
        response.setCodigo(activo.getCodigo());
        response.setNombre(activo.getNombre());
        response.setDescripcion(activo.getDescripcion());
        response.setUbicacion(activo.getUbicacion());
        response.setUrl_img(activo.getUrl_img());
        return response;
    } 

}
