package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.EspecialidadRequest;
import cfbd.co.sgt.dto.response.EspecialidadResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EspecialidadService {
    EspecialidadResponse crearEspecialidad(EspecialidadRequest especialidad);
    EspecialidadResponse editarEspecialidad(EspecialidadRequest especialidad, UUID id);
    List<EspecialidadResponse> listarEspecialidades();
    Optional<EspecialidadResponse> buscarEspecialidadPorId(UUID id);
    void eliminarEspecialidad(UUID id);
}
