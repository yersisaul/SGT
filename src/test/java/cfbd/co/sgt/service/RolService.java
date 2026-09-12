package cfbd.co.sgt.service;

import cfbd.co.sgt.model.Rol;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolService {
    Rol crearRol(Rol rol);
    Rol editarRol(Rol rol);
    List<Rol> listarRoles();
    Optional<Rol> buscarRolPorId(UUID id);
    Optional<Rol> buscarRolPorNombre(String nombre);
    void eliminarRol(UUID id);
}