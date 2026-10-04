package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.HistorialRequerimientoRequest;
import cfbd.co.sgt.dto.response.HistorialRequerimientoResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistorialRequerimientoService {
    HistorialRequerimientoResponse crearHistorial(HistorialRequerimientoRequest historial);
    /** idPadre opcional: si llega, solo el historial de ese registro (validando su visibilidad). */
    List<HistorialRequerimientoResponse> listarHistoriales(UUID idPadre);
    Optional<HistorialRequerimientoResponse> buscarHistorialPorId(UUID id);
}
