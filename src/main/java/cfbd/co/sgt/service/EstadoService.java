package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.EstadoRequest;
import cfbd.co.sgt.dto.response.EstadoResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstadoService {
    EstadoResponse crearEstado(EstadoRequest estado);
    EstadoResponse editarEstado(EstadoRequest estado, UUID id);
    List<EstadoResponse> listarEstados();
    Optional<EstadoResponse> buscarEstadoPorId(UUID id);
    void eliminarEstado(UUID id);
}
