package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.AprobacionRequest;
import cfbd.co.sgt.dto.response.AprobacionResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AprobacionService {
    AprobacionResponse crearAprobacion(AprobacionRequest aprobacion);
    List<AprobacionResponse> listarAprobaciones();
    Optional<AprobacionResponse> buscarAprobacionPorId(UUID id);
}
