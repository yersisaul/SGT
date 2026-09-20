package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cfbd.co.sgt.dto.request.RolPermisoRequest;
import cfbd.co.sgt.dto.response.RolPermisoResponse;
import cfbd.co.sgt.exception.DuplicateResourceException;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.model.Permiso;
import cfbd.co.sgt.model.Rol;
import cfbd.co.sgt.model.RolPermiso;
import cfbd.co.sgt.repository.PermisoRepository;
import cfbd.co.sgt.repository.RolPermisoRepository;
import cfbd.co.sgt.repository.RolRepository;
import cfbd.co.sgt.service.RolPermisoService;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class RolPermisoServiceImpl implements RolPermisoService {

    @Autowired
    private RolPermisoRepository rolPermisoRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PermisoRepository permisoRepository;

    @Override
    public RolPermisoResponse asignarPermiso(RolPermisoRequest rolPermisoDTO) {
        Rol rol = rolRepository.findById(rolPermisoDTO.getId_rol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol not found"));
        Permiso permiso = permisoRepository.findById(rolPermisoDTO.getId_permiso())
                .orElseThrow(() -> new ResourceNotFoundException("Permiso not found"));

        if (rolPermisoRepository.existsByRolAndPermiso(rol, permiso)) {
            throw new DuplicateResourceException("El rol ya tiene asignado ese permiso");
        }

        RolPermiso rolPermiso = new RolPermiso();
        rolPermiso.setRol(rol);
        rolPermiso.setPermiso(permiso);
        return convertToResponse(rolPermisoRepository.save(rolPermiso));
    }

    @Override
    public List<RolPermisoResponse> listarRolPermisos() {
        return rolPermisoRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RolPermisoResponse> buscarRolPermisoPorId(UUID id) {
        return rolPermisoRepository.findById(id).map(this::convertToResponse);
    }

    @Override
    public void revocarPermiso(UUID id) {
        rolPermisoRepository.deleteById(id);
    }

    private RolPermisoResponse convertToResponse(RolPermiso rolPermiso) {
        RolPermisoResponse response = new RolPermisoResponse();
        response.setId_rol_permiso(rolPermiso.getId_rol_permiso());
        response.setId_rol(rolPermiso.getRol().getId_rol());
        response.setNombre_rol(rolPermiso.getRol().getNombre());
        response.setId_permiso(rolPermiso.getPermiso().getId_permiso());
        response.setCodigo_permiso(rolPermiso.getPermiso().getCodigo());
        return response;
    }
}
