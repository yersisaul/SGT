package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import cfbd.co.sgt.service.RequerimientoService;
import cfbd.co.sgt.repository.RequerimientoRepository;
import cfbd.co.sgt.model.Requerimiento;
import java.util.UUID;

@Service 
@Transactional 
public class RequerimientoServiceImpl implements RequerimientoService {

    @Autowired
    private RequerimientoRepository requerimientoRepository;

    @Override
    public Requerimiento crearRequerimiento(Requerimiento requerimiento) {
        return requerimientoRepository.save(requerimiento);
    }

    @Override
    public Requerimiento editarRequerimiento(Requerimiento requerimiento) {
        return requerimientoRepository.save(requerimiento);
    }

    @Override
    public List<Requerimiento> listarRequerimientos() {
        return requerimientoRepository.findAll();
    }

    @Override
    public Optional<Requerimiento> buscarRequerimientoPorId(UUID id) {
        return requerimientoRepository.findById(id);
    }

    @Override
    public Optional<Requerimiento> buscarRequerimientoPorNumero(String numeroRequerimiento) {
        return requerimientoRepository.findByNumeroRequerimiento(numeroRequerimiento);
    }

    @Override
    public void eliminarRequerimiento(UUID id) {
        requerimientoRepository.deleteById(id);
    }
}