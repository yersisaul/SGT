package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import cfbd.co.sgt.service.SolicitudService;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.model.Solicitud;
import java.util.UUID;

@Service 
@Transactional 
public class SolicitudServiceImpl implements SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Override
    public Solicitud crearSolicitud(Solicitud solicitud) {
        return solicitudRepository.save(solicitud);
    }

    @Override
    public Solicitud editarSolicitud(Solicitud solicitud) {
        return solicitudRepository.save(solicitud);
    }

    @Override
    public List<Solicitud> listarSolicitudes() {
        return solicitudRepository.findAll();
    }

    @Override
    public Optional<Solicitud> buscarSolicitudPorId(UUID id) {
        return solicitudRepository.findById(id);
    }

    @Override
    public Optional<Solicitud> buscarSolicitudPorNumero(String numeroSolicitud) {
        return solicitudRepository.findByNumeroSolicitud(numeroSolicitud);
    }

    @Override
    public void eliminarSolicitud(UUID id) {
        solicitudRepository.deleteById(id);
    }
}