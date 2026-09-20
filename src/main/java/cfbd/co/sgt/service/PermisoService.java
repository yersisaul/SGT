package cfbd.co.sgt.service;

import cfbd.co.sgt.model.Permiso;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermisoService {
    Permiso crearPermiso(Permiso permiso);
    Permiso editarPermiso(Permiso permiso);
    List<Permiso> listarPermisos();
    Optional<Permiso> buscarPermisoPorId(UUID id);
    Optional<Permiso> buscarPermisoPorCodigo(String codigo);
    void eliminarPermiso(UUID id);
}