package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.AprobacionRequest;
import cfbd.co.sgt.dto.response.AprobacionResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface AprobacionService {
    AprobacionResponse crearAprobacion(AprobacionRequest aprobacion);
    List<AprobacionResponse> listarAprobaciones();
    Optional<AprobacionResponse> buscarAprobacionPorId(UUID id);

    AprobacionResponse subirAdjunto(UUID id, MultipartFile file);
    String obtenerReferenciaAdjunto(UUID id);
    AprobacionResponse eliminarAdjunto(UUID id);
}
