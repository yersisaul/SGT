package cfbd.co.sgt.service;
import cfbd.co.sgt.dto.request.ActivoRequest;
import cfbd.co.sgt.dto.response.ActivoResponse; 
import cfbd.co.sgt.model.Activo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivoService {
    Activo crearActivo(ActivoRequest activo);
    Activo editarActivo(ActivoRequest activo, UUID id);
    List<ActivoResponse> listarActivos();
    Optional<ActivoResponse> buscarActivoPorId(UUID id);
    void eliminarActivo(UUID id);
}
