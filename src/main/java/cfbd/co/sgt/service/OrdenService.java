package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.CerrarOrdenRequest;
import cfbd.co.sgt.dto.request.OrdenRequest;
import cfbd.co.sgt.dto.request.ReasignarOrdenRequest;
import cfbd.co.sgt.dto.response.OrdenResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface OrdenService {
    OrdenResponse crearOrden(OrdenRequest orden);
    OrdenResponse editarOrden(OrdenRequest orden, UUID id);
    List<OrdenResponse> listarOrdenes();
    Optional<OrdenResponse> buscarOrdenPorId(UUID id);
    Optional<OrdenResponse> buscarOrdenPorNumero(String numeroOrden);
    void eliminarOrden(UUID id);
    OrdenResponse cerrarOrden(UUID id, CerrarOrdenRequest request);
    OrdenResponse reasignarOrden(UUID id, ReasignarOrdenRequest request);

    OrdenResponse subirAdjunto(UUID id, MultipartFile file);
    String obtenerReferenciaAdjunto(UUID id);
    OrdenResponse eliminarAdjunto(UUID id);
}
