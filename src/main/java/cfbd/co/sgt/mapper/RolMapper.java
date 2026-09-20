package cfbd.co.sgt.mapper;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.request.RolRequest;
import cfbd.co.sgt.dto.response.RolResponse;
import cfbd.co.sgt.model.Rol;

@Component
public class RolMapper {

    public Rol toEntity(RolRequest request) {
        Rol rol = new Rol();
        rol.setNombre(request.getNombre());
        rol.setDescripcion(request.getDescripcion());
        return rol;
    }

    public Rol updateEntity(Rol rol, RolRequest request) {
        rol.setNombre(request.getNombre());
        rol.setDescripcion(request.getDescripcion());
        return rol;
    }

    public RolResponse toResponse(Rol rol) {
        if (rol == null) {
            return null;
        }
        RolResponse response = new RolResponse();
        response.setId_rol(rol.getId_rol());
        response.setNombre(rol.getNombre());
        response.setDescripcion(rol.getDescripcion());
        return response;
    }
}
