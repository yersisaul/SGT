package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.HistorialSolicitudRequest;
import cfbd.co.sgt.dto.response.HistorialSolicitudResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistorialSolicitudService {
    HistorialSolicitudResponse crearHistorial(HistorialSolicitudRequest historial);
    /** idPadre opcional: si llega, solo el historial de ese registro (validando su visibilidad). */
    List<HistorialSolicitudResponse> listarHistoriales(UUID idPadre);
    Optional<HistorialSolicitudResponse> buscarHistorialPorId(UUID id);
}
