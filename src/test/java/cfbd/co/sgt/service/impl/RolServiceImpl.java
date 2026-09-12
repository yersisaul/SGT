package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import cfbd.co.sgt.service.RolService;
import cfbd.co.sgt.repository.RolRepository;
import cfbd.co.sgt.model.Rol;
import java.util.UUID;

@Service 
@Transactional 
public class RolServiceImpl implements RolService {

    @Autowired
    private RolRepository rolRepository;

    @Override
    public Rol crearRol(Rol rol) {
        return rolRepository.save(rol);
    }

    @Override
    public Rol editarRol(Rol rol) {
        return rolRepository.save(rol);
    }

    @Override
    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    @Override
    public Optional<Rol> buscarRolPorId(UUID id) {
        return rolRepository.findById(id);
    }

    @Override
    public Optional<Rol> buscarRolPorNombre(String nombre) {
        return rolRepository.findByNombre(nombre);
    }

    @Override
    public void eliminarRol(UUID id) {
        rolRepository.deleteById(id);
    }
}