package cfbd.co.sgt.service;

import cfbd.co.sgt.model.Requerimiento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequerimientoService {
    Requerimiento crearRequerimiento(Requerimiento requerimiento);
    Requerimiento editarRequerimiento(Requerimiento requerimiento);
    List<Requerimiento> listarRequerimientos();
    Optional<Requerimiento> buscarRequerimientoPorId(UUID id);
    Optional<Requerimiento> buscarRequerimientoPorNumero(String numeroRequerimiento);
    void eliminarRequerimiento(UUID id);
}