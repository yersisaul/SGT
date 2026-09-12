package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import cfbd.co.sgt.service.PermisoService;
import cfbd.co.sgt.repository.PermisoRepository;
import cfbd.co.sgt.model.Permiso;
import java.util.UUID;

@Service 
@Transactional 
public class PermisoServiceImpl implements PermisoService {

    @Autowired
    private PermisoRepository permisoRepository;

    @Override
    public Permiso crearPermiso(Permiso permiso) {
        return permisoRepository.save(permiso);
    }

    @Override
    public Permiso editarPermiso(Permiso permiso) {
        return permisoRepository.save(permiso);
    }

    @Override
    public List<Permiso> listarPermisos() {
        return permisoRepository.findAll();
    }

    @Override
    public Optional<Permiso> buscarPermisoPorId(UUID id) {
        return permisoRepository.findById(id);
    }

    @Override
    public Optional<Permiso> buscarPermisoPorCodigo(String codigo) {
        return permisoRepository.findByCodigo(codigo);
    }

    @Override
    public void eliminarPermiso(UUID id) {
        permisoRepository.deleteById(id);
    }
}