package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;
import cfbd.co.sgt.service.ActivoService;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import cfbd.co.sgt.repository.ActivoRepository;
import cfbd.co.sgt.dto.request.ActivoRequest;
import cfbd.co.sgt.dto.response.ActivoResponse;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.model.Activo;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ActivoServiceImpl implements ActivoService {
    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public ActivoResponse crearActivo(ActivoRequest activoDTO) {
        Activo activo = new Activo();
        activo.setEspecialidad(especialidadRepository.findById(activoDTO.getId_especialidad()).orElse(null));
        activo.setCodigo(activoDTO.getCodigo());
        activo.setNombre(activoDTO.getNombre());
        activo.setDescripcion(activoDTO.getDescripcion());
        activo.setUbicacion(activoDTO.getUbicacion());
        // La imagen se gestiona exclusivamente vía subirImagen/eliminarImagen
        // (fileserver propio, CLAUDE.md sección 26/30): no se acepta desde este DTO.
        return convertToResponse(activoRepository.save(activo));
    }

    @Override
    public ActivoResponse editarActivo(ActivoRequest activoDTO, UUID id) {
        Activo activo = activoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activo not found"));
        activo.setEspecialidad(especialidadRepository.findById(activoDTO.getId_especialidad()).orElse(null));
        activo.setCodigo(activoDTO.getCodigo());
        activo.setNombre(activoDTO.getNombre());
        activo.setDescripcion(activoDTO.getDescripcion());
        activo.setUbicacion(activoDTO.getUbicacion());
        return convertToResponse(activoRepository.save(activo));
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

    @Override
    public ActivoResponse subirImagen(UUID id, MultipartFile file) {
        Activo activo = activoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activo not found"));
        String referenciaAnterior = activo.getUrl_img();
        // Guardar el nuevo archivo antes de tocar BD/borrar el anterior
        // (CLAUDE.md 30: nunca dejar la entidad apuntando a un archivo
        // inexistente si algo falla a mitad de camino).
        String nuevaReferencia = fileStorageService.store(file, TipoRecursoArchivo.ACTIVOS, id);
        activo.setUrl_img(nuevaReferencia);
        Activo guardado = activoRepository.save(activo);
        if (referenciaAnterior != null) {
            fileStorageService.delete(referenciaAnterior);
        }
        return convertToResponse(guardado);
    }

    @Override
    public String obtenerReferenciaImagen(UUID id) {
        Activo activo = activoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activo not found"));
        if (activo.getUrl_img() == null) {
            throw new ResourceNotFoundException("El activo no tiene imagen.");
        }
        return activo.getUrl_img();
    }

    @Override
    public ActivoResponse eliminarImagen(UUID id) {
        Activo activo = activoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activo not found"));
        if (activo.getUrl_img() != null) {
            fileStorageService.delete(activo.getUrl_img());
            activo.setUrl_img(null);
            activoRepository.save(activo);
        }
        return convertToResponse(activo);
    }

    private ActivoResponse convertToResponse(Activo activo) {
        ActivoResponse response = new ActivoResponse();
        response.setId_activo(activo.getId_activo());
        response.setId_especialidad(activo.getEspecialidad().getId_especialidad());
        response.setCodigo(activo.getCodigo());
        response.setNombre(activo.getNombre());
        response.setDescripcion(activo.getDescripcion());
        response.setUbicacion(activo.getUbicacion());
        response.setUrl_img(activo.getUrl_img() != null ? "/api/archivos/activos/" + activo.getId_activo() : null);
        return response;
    }

}
