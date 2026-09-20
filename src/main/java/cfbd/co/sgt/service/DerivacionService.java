package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.DerivacionRequest;
import cfbd.co.sgt.dto.response.DerivacionResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DerivacionService {
    DerivacionResponse crearDerivacion(DerivacionRequest derivacion);
    List<DerivacionResponse> listarDerivaciones();
    Optional<DerivacionResponse> buscarDerivacionPorId(UUID id);
}
