package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.WebClienteReferenciaRequest;
import cfbd.co.sgt.dto.response.WebClienteReferenciaPublicoResponse;
import cfbd.co.sgt.dto.response.WebClienteReferenciaResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.mapper.WebClienteReferenciaMapper;
import cfbd.co.sgt.model.WebClienteReferencia;
import cfbd.co.sgt.repository.WebClienteReferenciaRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import cfbd.co.sgt.service.WebClienteReferenciaService;
import cfbd.co.sgt.service.WebTextos;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebClienteReferenciaServiceImpl implements WebClienteReferenciaService {

    private static final String NO_ENCONTRADO = "Cliente de referencia no encontrado";

    private final WebClienteReferenciaRepository clienteRepository;
    private final WebClienteReferenciaMapper clienteMapper;
    private final FileStorageService fileStorageService;
    private final UsuarioActualProvider usuarioActualProvider;

    @Override
    @Transactional(readOnly = true)
    public List<WebClienteReferenciaResponse> listar() {
        return clienteRepository.findAllOrdenados().stream().map(clienteMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebClienteReferenciaResponse buscarPorId(UUID id) {
        return clienteMapper.toResponse(obtener(id));
    }

    @Override
    @Transactional
    public WebClienteReferenciaResponse crear(WebClienteReferenciaRequest request) {
        WebClienteReferencia cliente = new WebClienteReferencia();
        aplicar(cliente, request);
        cliente.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        cliente.setPublicado(Boolean.TRUE.equals(request.getPublicado()));
        return guardar(cliente);
    }

    @Override
    @Transactional
    public WebClienteReferenciaResponse editar(UUID id, WebClienteReferenciaRequest request) {
        WebClienteReferencia cliente = obtener(id);
        aplicar(cliente, request);
        if (request.getOrden() != null) {
            cliente.setOrden(request.getOrden());
        }
        if (request.getPublicado() != null) {
            cliente.setPublicado(request.getPublicado());
        }
        return guardar(cliente);
    }

    @Override
    @Transactional
    public WebClienteReferenciaResponse cambiarPublicacion(UUID id, boolean publicado) {
        WebClienteReferencia cliente = obtener(id);
        cliente.setPublicado(publicado);
        return guardar(cliente);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        WebClienteReferencia cliente = obtener(id);
        String logo = cliente.getUrl_logo();
        clienteRepository.delete(cliente);
        // flush aquí: si un caso de éxito lo referencia, el 409 sale antes de borrar el logo.
        clienteRepository.flush();
        fileStorageService.delete(logo);
    }

    @Override
    @Transactional
    public WebClienteReferenciaResponse subirLogo(UUID id, MultipartFile file) {
        WebClienteReferencia cliente = obtener(id);
        String referenciaAnterior = cliente.getUrl_logo();
        cliente.setUrl_logo(fileStorageService.store(file, TipoRecursoArchivo.WEB_CLIENTES, id));
        WebClienteReferenciaResponse response = guardar(cliente);
        fileStorageService.delete(referenciaAnterior);
        return response;
    }

    @Override
    @Transactional
    public WebClienteReferenciaResponse eliminarLogo(UUID id) {
        WebClienteReferencia cliente = obtener(id);
        String referencia = cliente.getUrl_logo();
        if (referencia == null) {
            return clienteMapper.toResponse(cliente);
        }
        cliente.setUrl_logo(null);
        WebClienteReferenciaResponse response = guardar(cliente);
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
    public List<WebClienteReferenciaPublicoResponse> listarPublicados() {
        return clienteRepository.findPublicados().stream().map(clienteMapper::toPublicoResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerReferenciaLogoPublica(UUID id) {
        // Un cliente no publicado responde igual que uno inexistente.
        WebClienteReferencia cliente = clienteRepository.findById(id)
                .filter(WebClienteReferencia::isPublicado)
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
        return referenciaLogo(cliente);
    }

    private WebClienteReferencia obtener(UUID id) {
        return clienteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
    }

    private String referenciaLogo(WebClienteReferencia cliente) {
        if (cliente.getUrl_logo() == null) {
            throw new ResourceNotFoundException("El cliente no tiene logo.");
        }
        return cliente.getUrl_logo();
    }

    private void aplicar(WebClienteReferencia cliente, WebClienteReferenciaRequest request) {
        cliente.setNombre(WebTextos.limpiar(request.getNombre()));
        cliente.setSector(WebTextos.limpiar(request.getSector()));
        cliente.setUrl_sitio(WebTextos.limpiar(request.getUrl_sitio()));
    }

    // saveAndFlush: dispara @PreUpdate antes de mapear, así la respuesta trae
    // la fecha_actualizacion (y la versión de la URL del logo) nueva.
    private WebClienteReferenciaResponse guardar(WebClienteReferencia cliente) {
        cliente.setUsuario_actualiza(usuarioActualProvider.obtener());
        return clienteMapper.toResponse(clienteRepository.saveAndFlush(cliente));
    }
}
