package cfbd.co.sgt.service;

import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.dto.request.OrdenRequest;
import cfbd.co.sgt.dto.response.OrdenResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdenService {
    Orden crearOrden(OrdenRequest orden);
    Orden editarOrden(OrdenRequest orden, UUID id);
    List<OrdenResponse> listarOrdenes();
    Optional<OrdenResponse> buscarOrdenPorId(UUID id);
    Optional<OrdenResponse> buscarOrdenPorNumero(String numeroOrden);
    void eliminarOrden(UUID id);
}