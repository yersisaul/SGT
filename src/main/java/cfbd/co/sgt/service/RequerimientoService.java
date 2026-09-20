package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.GenerarOrdenRequest;
import cfbd.co.sgt.dto.request.RequerimientoRequest;
import cfbd.co.sgt.dto.response.OrdenResponse;
import cfbd.co.sgt.dto.response.RequerimientoResponse;
import cfbd.co.sgt.dto.response.ResumenEstadosResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequerimientoService {
    RequerimientoResponse crearRequerimiento(RequerimientoRequest requerimiento);
    RequerimientoResponse editarRequerimiento(RequerimientoRequest requerimiento, UUID id);
    List<RequerimientoResponse> listarRequerimientos();
    Optional<RequerimientoResponse> buscarRequerimientoPorId(UUID id);
    Optional<RequerimientoResponse> buscarRequerimientoPorNumero(String numeroRequerimiento);
    void eliminarRequerimiento(UUID id);
    ResumenEstadosResponse obtenerResumenPorEstado();
    OrdenResponse generarOrdenDesdeRequerimiento(UUID idRequerimiento, GenerarOrdenRequest request);
}
