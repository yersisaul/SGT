package cfbd.co.sgt.mapper;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.request.PermisoRequest;
import cfbd.co.sgt.dto.response.PermisoResponse;
import cfbd.co.sgt.model.Permiso;

@Component
public class PermisoMapper {

    public Permiso toEntity(PermisoRequest request) {
        Permiso permiso = new Permiso();
        permiso.setCodigo(request.getCodigo());
        permiso.setDescripcion(request.getDescripcion());
        return permiso;
    }

    public Permiso updateEntity(Permiso permiso, PermisoRequest request) {
        permiso.setCodigo(request.getCodigo());
        permiso.setDescripcion(request.getDescripcion());
        return permiso;
    }

    public PermisoResponse toResponse(Permiso permiso) {
        if (permiso == null) {
            return null;
        }
        PermisoResponse response = new PermisoResponse();
        response.setId_permiso(permiso.getId_permiso());
        response.setCodigo(permiso.getCodigo());
        response.setDescripcion(permiso.getDescripcion());
        return response;
    }
}
