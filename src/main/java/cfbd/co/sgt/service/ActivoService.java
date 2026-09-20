package cfbd.co.sgt.service;
import cfbd.co.sgt.dto.request.ActivoRequest;
import cfbd.co.sgt.dto.response.ActivoResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivoService {
    ActivoResponse crearActivo(ActivoRequest activo);
    ActivoResponse editarActivo(ActivoRequest activo, UUID id);
    List<ActivoResponse> listarActivos();
    Optional<ActivoResponse> buscarActivoPorId(UUID id);
    void eliminarActivo(UUID id);
}
