package cfbd.co.sgt.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;
import cfbd.co.sgt.service.ActivoService;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import cfbd.co.sgt.model.ActivoEspecialidad;
import cfbd.co.sgt.repository.ActivoEspecialidadRepository;
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

    @Autowired
    private ActivoEspecialidadRepository activoEspecialidadRepository;

    @Override
    public ActivoResponse crearActivo(ActivoRequest activoDTO) {
        Activo activo = new Activo();
        activo.setEspecialidad(especialidadRepository.findById(activoDTO.getId_especialidad())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found")));
        activo.setCodigo(activoDTO.getCodigo());
        activo.setNombre(activoDTO.getNombre());
        activo.setDescripcion(activoDTO.getDescripcion());
        activo.setUbicacion(activoDTO.getUbicacion());
        // La imagen se gestiona exclusivamente vía subirImagen/eliminarImagen no se acepta desde este DTO.
        Activo guardado = activoRepository.save(activo);
        reemplazarEspecialidades(guardado, activoDTO.getIds_especialidad());
        return convertToResponse(guardado);
    }

    @Override
    public ActivoResponse editarActivo(ActivoRequest activoDTO, UUID id) {
        Activo activo = activoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activo not found"));
        activo.setEspecialidad(especialidadRepository.findById(activoDTO.getId_especialidad())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found")));
        activo.setCodigo(activoDTO.getCodigo());
        activo.setNombre(activoDTO.getNombre());
        activo.setDescripcion(activoDTO.getDescripcion());
        activo.setUbicacion(activoDTO.getUbicacion());
        Activo guardado = activoRepository.save(activo);
        reemplazarEspecialidades(guardado, activoDTO.getIds_especialidad());
        return convertToResponse(guardado);
    }

    @Override
    public List<ActivoResponse> listarActivos() {
        List<Activo> activos = activoRepository.findAll();
        Map<UUID, List<UUID>> especialidades = new HashMap<>();
        for (Object[] par : activoEspecialidadRepository.findTodosLosPares()) {
            especialidades.computeIfAbsent((UUID) par[0], id -> new ArrayList<>()).add((UUID) par[1]);
        }
        return activos.stream()
                .map(activo -> convertToResponse(activo, especialidades.getOrDefault(activo.getId_activo(), List.of())))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ActivoResponse> buscarActivoPorId(UUID id) {
        return activoRepository.findById(id).map(this::convertToResponse);
    }

    @Override
    public void eliminarActivo(UUID id) {
        activoEspecialidadRepository.deleteByActivo(id);
        activoRepository.deleteById(id);
    }

    @Override
    public ActivoResponse subirImagen(UUID id, MultipartFile file) {
        Activo activo = activoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activo not found"));
        String referenciaAnterior = activo.getUrl_img();
        // Guardar el nuevo archivo antes de tocar BD/borrar el anterior
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

    /** La principal siempre forma parte del conjunto; se reemplaza el conjunto completo. */
    private void reemplazarEspecialidades(Activo activo, List<UUID> idsEspecialidad) {
        Set<UUID> ids = new LinkedHashSet<>();
        ids.add(activo.getEspecialidad().getId_especialidad());
        if (idsEspecialidad != null) {
            ids.addAll(idsEspecialidad);
        }
        activoEspecialidadRepository.deleteByActivo(activo.getId_activo());
        activoEspecialidadRepository.flush();
        for (UUID idEspecialidad : ids) {
            ActivoEspecialidad relacion = new ActivoEspecialidad();
            relacion.setActivo(activo);
            relacion.setEspecialidad(especialidadRepository.findById(idEspecialidad)
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad not found")));
            activoEspecialidadRepository.save(relacion);
        }
    }

    private ActivoResponse convertToResponse(Activo activo) {
        return convertToResponse(activo, activoEspecialidadRepository.findIdsEspecialidad(activo.getId_activo()));
    }

    private ActivoResponse convertToResponse(Activo activo, List<UUID> idsEspecialidad) {
        ActivoResponse response = new ActivoResponse();
        response.setId_activo(activo.getId_activo());
        response.setId_especialidad(activo.getEspecialidad().getId_especialidad());
        // Activos anteriores a la tabla N:M solo tienen la principal.
        response.setIds_especialidad(idsEspecialidad.isEmpty()
                ? List.of(activo.getEspecialidad().getId_especialidad()) : idsEspecialidad);
        response.setCodigo(activo.getCodigo());
        response.setNombre(activo.getNombre());
        response.setDescripcion(activo.getDescripcion());
        response.setUbicacion(activo.getUbicacion());
        response.setUrl_img(activo.getUrl_img() != null ? "/api/archivos/activos/" + activo.getId_activo() : null);
        return response;
    }

}
