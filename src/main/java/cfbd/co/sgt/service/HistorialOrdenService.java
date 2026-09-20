package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.HistorialOrdenRequest;
import cfbd.co.sgt.dto.response.HistorialOrdenResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistorialOrdenService {
    HistorialOrdenResponse crearHistorial(HistorialOrdenRequest historial);
    List<HistorialOrdenResponse> listarHistoriales();
    Optional<HistorialOrdenResponse> buscarHistorialPorId(UUID id);
}
