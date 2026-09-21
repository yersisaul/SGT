package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.GenerarOrdenRequest;
import cfbd.co.sgt.dto.request.GenerarRequerimientoRequest;
import cfbd.co.sgt.dto.request.SolicitudRequest;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.dto.response.RequerimientoResponse;
import cfbd.co.sgt.dto.response.ResumenEstadosResponse;
import cfbd.co.sgt.dto.response.SolicitudResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitudService {
    SolicitudResponse crearSolicitud(SolicitudRequest solicitud);
    SolicitudResponse editarSolicitud(SolicitudRequest solicitud, UUID id);
    List<SolicitudResponse> listarSolicitudes();
    Optional<SolicitudResponse> buscarSolicitudPorId(UUID id);
    Optional<SolicitudResponse> buscarSolicitudPorNumero(String numeroSolicitud);
    void eliminarSolicitud(UUID id);
    ResumenEstadosResponse obtenerResumenPorEstado();
    OrdenResponse generarOrdenDesdeSolicitud(UUID idSolicitud, GenerarOrdenRequest request);
    RequerimientoResponse generarRequerimientoDesdeSolicitud(UUID idSolicitud, GenerarRequerimientoRequest request);
}
