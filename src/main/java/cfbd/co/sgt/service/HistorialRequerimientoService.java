package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.HistorialRequerimientoRequest;
import cfbd.co.sgt.dto.response.HistorialRequerimientoResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistorialRequerimientoService {
    HistorialRequerimientoResponse crearHistorial(HistorialRequerimientoRequest historial);
    List<HistorialRequerimientoResponse> listarHistoriales();
    Optional<HistorialRequerimientoResponse> buscarHistorialPorId(UUID id);
}
