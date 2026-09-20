package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.HistorialSolicitudRequest;
import cfbd.co.sgt.dto.response.HistorialSolicitudResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistorialSolicitudService {
    HistorialSolicitudResponse crearHistorial(HistorialSolicitudRequest historial);
    List<HistorialSolicitudResponse> listarHistoriales();
    Optional<HistorialSolicitudResponse> buscarHistorialPorId(UUID id);
}
