package cfbd.co.sgt.service;

import cfbd.co.sgt.model.Solicitud;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SolicitudService {
    Solicitud crearSolicitud(Solicitud solicitud);
    Solicitud editarSolicitud(Solicitud solicitud);
    List<Solicitud> listarSolicitudes();
    Optional<Solicitud> buscarSolicitudPorId(UUID id);
    Optional<Solicitud> buscarSolicitudPorNumero(String numeroSolicitud);
    void eliminarSolicitud(UUID id);
}