package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import cfbd.co.sgt.service.OrdenService;
import cfbd.co.sgt.repository.OrdenRepository;
import cfbd.co.sgt.repository.UsuarioRepository;
import cfbd.co.sgt.repository.EstadoRepository;
import cfbd.co.sgt.repository.EspecialidadRepository;
import cfbd.co.sgt.repository.SolicitudRepository;
import cfbd.co.sgt.repository.RequerimientoRepository;
import cfbd.co.sgt.model.Orden;
import cfbd.co.sgt.dto.request.OrdenRequest;
import cfbd.co.sgt.dto.response.OrdenResponse;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.Instant;

@Service 
@Transactional 
public class OrdenServiceImpl implements OrdenService {
    @Autowired
    private OrdenRepository ordenRepository;

    @Autowired 
    private UsuarioRepository usuarioRepository;

    @Autowired 
    private EstadoRepository estadoRepository;

    @Autowired 
    private EspecialidadRepository especialidadRepository;

    @Autowired 
    private SolicitudRepository solicitudRepository;

    @Autowired 
    private RequerimientoRepository requerimientoRepository;

    @Override
    public Orden crearOrden(OrdenRequest ordenDTO) {
        Orden orden = new Orden();
        orden.setUsuario(usuarioRepository.findById(ordenDTO.getId_usuario()).orElse(null));
        orden.setEstado(estadoRepository.findById(ordenDTO.getId_estado()).orElse(null));
        orden.setEspecialidad(especialidadRepository.findById(ordenDTO.getId_especialidad()).orElse(null));
        orden.setSolicitud(solicitudRepository.findById(ordenDTO.getId_solicitud()).orElse(null));
        orden.setRequerimiento(requerimientoRepository.findById(ordenDTO.getId_requerimiento()).orElse(null));
        orden.setUrl_adjunto(ordenDTO.getUrl_adjunto());
        Long correlativo = ordenRepository.count() + 1;
        orden.setNumeroOrden("OT-"+Long.toString(correlativo));
        orden.setFecha_registro(Instant.now());
        return ordenRepository.save(orden);
    }

    @Override
    public Orden editarOrden(OrdenRequest ordenDTO, UUID id) {
        Orden orden = ordenRepository.findById(null).orElse(null);
        orden.setUsuario(usuarioRepository.findById(ordenDTO.getId_usuario()).orElse(null));
        orden.setEstado(estadoRepository.findById(ordenDTO.getId_estado()).orElse(null));
        orden.setEspecialidad(especialidadRepository.findById(ordenDTO.getId_especialidad()).orElse(null));
        orden.setSolicitud(solicitudRepository.findById(ordenDTO.getId_solicitud()).orElse(null));
        orden.setRequerimiento(requerimientoRepository.findById(ordenDTO.getId_requerimiento()).orElse(null));
        orden.setUrl_adjunto(ordenDTO.getUrl_adjunto());        
        return ordenRepository.save(orden);
    }

    @Override
    public List<OrdenResponse> listarOrdenes() {
        List<Orden> ordenes = ordenRepository.findAll();
        return ordenes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());        
    }

    @Override
    public Optional<OrdenResponse> buscarOrdenPorId(UUID id) {
        return ordenRepository.findById(id).map(this::convertToResponse);
    }

    @Override
    public Optional<OrdenResponse> buscarOrdenPorNumero(String numeroOrden) {
        return ordenRepository.findByNumeroOrden(numeroOrden).map(this::convertToResponse);
    }

    @Override
    public void eliminarOrden(UUID id) {
        ordenRepository.deleteById(id);
    }

    private OrdenResponse convertToResponse(Orden orden){
        OrdenResponse response = new OrdenResponse();
        response.setId_orden(orden.getId_orden());
        response.setId_especialidad(orden.getEspecialidad().getId_especialidad());
        response.setId_estado(orden.getEstado().getId_estado());
        response.setId_requerimiento(orden.getRequerimiento().getId_requerimiento());
        response.setId_solicitud(orden.getSolicitud().getId_solicitud());
        response.setNumeroOrden(orden.getNumeroOrden());
        response.setFecha_registro(orden.getFecha_registro());
        response.setFecha_cierre(orden.getFecha_cierre());
        response.setUrl_adjunto(orden.getUrl_adjunto());
        return response;
    }
}