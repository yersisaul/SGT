package cfbd.co.sgt.service;

import cfbd.co.sgt.dto.request.RolPermisoRequest;
import cfbd.co.sgt.dto.response.RolPermisoResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolPermisoService {
    RolPermisoResponse asignarPermiso(RolPermisoRequest rolPermiso);
    List<RolPermisoResponse> listarRolPermisos();
    Optional<RolPermisoResponse> buscarRolPermisoPorId(UUID id);
    void revocarPermiso(UUID id);
}
