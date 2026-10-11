package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.WebPartnerRequest;
import cfbd.co.sgt.dto.response.WebPartnerPublicoResponse;
import cfbd.co.sgt.dto.response.WebPartnerResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.mapper.WebPartnerMapper;
import cfbd.co.sgt.model.TipoPartner;
import cfbd.co.sgt.model.WebPartner;
import cfbd.co.sgt.repository.WebPartnerRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import cfbd.co.sgt.service.WebPartnerService;
import cfbd.co.sgt.service.WebTextos;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebPartnerServiceImpl implements WebPartnerService {

    private static final String NO_ENCONTRADO = "Partner no encontrado";

    private final WebPartnerRepository partnerRepository;
    private final WebPartnerMapper partnerMapper;
    private final FileStorageService fileStorageService;
    private final UsuarioActualProvider usuarioActualProvider;

    @Override
    @Transactional(readOnly = true)
    public List<WebPartnerResponse> listar() {
        return partnerRepository.findAllOrdenados().stream().map(partnerMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebPartnerResponse buscarPorId(UUID id) {
        return partnerMapper.toResponse(obtener(id));
    }

    @Override
    @Transactional
    public WebPartnerResponse crear(WebPartnerRequest request) {
        WebPartner partner = new WebPartner();
        aplicar(partner, request);
        partner.setTipo(request.getTipo() != null ? request.getTipo() : TipoPartner.TECNOLOGICO);
        partner.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        partner.setPublicado(Boolean.TRUE.equals(request.getPublicado()));
        return guardar(partner);
    }

    @Override
    @Transactional
    public WebPartnerResponse editar(UUID id, WebPartnerRequest request) {
        WebPartner partner = obtener(id);
        aplicar(partner, request);
        if (request.getTipo() != null) {
            partner.setTipo(request.getTipo());
        }
        if (request.getOrden() != null) {
            partner.setOrden(request.getOrden());
        }
        if (request.getPublicado() != null) {
            partner.setPublicado(request.getPublicado());
        }
        return guardar(partner);
    }

    @Override
    @Transactional
    public WebPartnerResponse cambiarPublicacion(UUID id, boolean publicado) {
        WebPartner partner = obtener(id);
        partner.setPublicado(publicado);
        return guardar(partner);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        WebPartner partner = obtener(id);
        String logo = partner.getUrl_logo();
        partnerRepository.delete(partner);
        // flush aquí: si otro registro lo referencia, el 409 sale antes de borrar el logo.
        partnerRepository.flush();
        fileStorageService.delete(logo);
    }

    @Override
    @Transactional
    public WebPartnerResponse subirLogo(UUID id, MultipartFile file) {
        WebPartner partner = obtener(id);
        String referenciaAnterior = partner.getUrl_logo();
        partner.setUrl_logo(fileStorageService.store(file, TipoRecursoArchivo.WEB_PARTNERS, id));
        WebPartnerResponse response = guardar(partner);
        fileStorageService.delete(referenciaAnterior);
        return response;
    }

    @Override
    @Transactional
    public WebPartnerResponse eliminarLogo(UUID id) {
        WebPartner partner = obtener(id);
        String referencia = partner.getUrl_logo();
        if (referencia == null) {
            return partnerMapper.toResponse(partner);
        }
        partner.setUrl_logo(null);
        WebPartnerResponse response = guardar(partner);
        fileStorageService.delete(referencia);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerReferenciaLogo(UUID id) {
        return referenciaLogo(obtener(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebPartnerPublicoResponse> listarPublicados(TipoPartner tipo) {
        return partnerRepository.findPublicados().stream()
                .filter(p -> tipo == null || p.getTipo() == tipo)
                .map(partnerMapper::toPublicoResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerReferenciaLogoPublica(UUID id) {
        // Un partner no publicado responde igual que uno inexistente.
        WebPartner partner = partnerRepository.findById(id)
                .filter(WebPartner::isPublicado)
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
        return referenciaLogo(partner);
    }

    private WebPartner obtener(UUID id) {
        return partnerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
    }

    private String referenciaLogo(WebPartner partner) {
        if (partner.getUrl_logo() == null) {
            throw new ResourceNotFoundException("El partner no tiene logo.");
        }
        return partner.getUrl_logo();
    }

    private void aplicar(WebPartner partner, WebPartnerRequest request) {
        partner.setNombre(WebTextos.limpiar(request.getNombre()));
        partner.setCategoria(WebTextos.limpiar(request.getCategoria()));
        partner.setDescripcion(WebTextos.limpiar(request.getDescripcion()));
        partner.setUrl_sitio(WebTextos.limpiar(request.getUrl_sitio()));
    }

    // saveAndFlush: dispara @PreUpdate antes de mapear, así la respuesta trae
    // la fecha_actualizacion (y la versión de la URL del logo) nueva.
    private WebPartnerResponse guardar(WebPartner partner) {
        partner.setUsuario_actualiza(usuarioActualProvider.obtener());
        return partnerMapper.toResponse(partnerRepository.saveAndFlush(partner));
    }
}
