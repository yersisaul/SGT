package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.HistorialOrdenRequest;
import cfbd.co.sgt.dto.response.HistorialOrdenResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistorialOrdenService {
    HistorialOrdenResponse crearHistorial(HistorialOrdenRequest historial);
    /** idPadre opcional: si llega, solo el historial de ese registro (validando su visibilidad). */
    List<HistorialOrdenResponse> listarHistoriales(UUID idPadre);
    Optional<HistorialOrdenResponse> buscarHistorialPorId(UUID id);
}
