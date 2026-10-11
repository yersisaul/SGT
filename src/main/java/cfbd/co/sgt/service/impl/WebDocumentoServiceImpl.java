package cfbd.co.sgt.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cfbd.co.sgt.dto.request.WebDocumentoRequest;
import cfbd.co.sgt.dto.response.WebDocumentoPublicoResponse;
import cfbd.co.sgt.dto.response.WebDocumentoResponse;
import cfbd.co.sgt.exception.ResourceNotFoundException;
import cfbd.co.sgt.mapper.WebDocumentoMapper;
import cfbd.co.sgt.model.TipoDocumentoWeb;
import cfbd.co.sgt.model.WebDocumento;
import cfbd.co.sgt.repository.WebDocumentoRepository;
import cfbd.co.sgt.repository.WebProductoRepository;
import cfbd.co.sgt.security.UsuarioActualProvider;
import cfbd.co.sgt.service.FileStorageService;
import cfbd.co.sgt.service.TipoRecursoArchivo;
import cfbd.co.sgt.service.WebDocumentoService;
import cfbd.co.sgt.service.WebTextos;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebDocumentoServiceImpl implements WebDocumentoService {

    private static final String NO_ENCONTRADO = "Documento no encontrado";
    private static final int LONGITUD_NOMBRE = 255;

    private final WebDocumentoRepository documentoRepository;
    private final WebProductoRepository productoRepository;
    private final WebDocumentoMapper documentoMapper;
    private final FileStorageService fileStorageService;
    private final UsuarioActualProvider usuarioActualProvider;

    @Override
    @Transactional(readOnly = true)
    public List<WebDocumentoResponse> listar() {
        return documentoRepository.findAllOrdenados().stream().map(documentoMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WebDocumentoResponse buscarPorId(UUID id) {
        return documentoMapper.toResponse(obtener(id));
    }

    @Override
    @Transactional
    public WebDocumentoResponse crear(WebDocumentoRequest request) {
        WebDocumento documento = new WebDocumento();
        aplicar(documento, request);
        documento.setTipo(request.getTipo() != null ? request.getTipo() : TipoDocumentoWeb.BROCHURE);
        documento.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        // Recién creado no tiene PDF: publicarlo aquí siempre sería un 409.
        setPublicado(documento, Boolean.TRUE.equals(request.getPublicado()));
        return guardar(documento);
    }

    @Override
    @Transactional
    public WebDocumentoResponse editar(UUID id, WebDocumentoRequest request) {
        WebDocumento documento = obtener(id);
        aplicar(documento, request);
        if (request.getTipo() != null) {
            documento.setTipo(request.getTipo());
        }
        if (request.getOrden() != null) {
            documento.setOrden(request.getOrden());
        }
        if (request.getPublicado() != null) {
            setPublicado(documento, request.getPublicado());
        }
        return guardar(documento);
    }

    @Override
    @Transactional
    public WebDocumentoResponse cambiarPublicacion(UUID id, boolean publicado) {
        WebDocumento documento = obtener(id);
        setPublicado(documento, publicado);
        return guardar(documento);
    }

    @Override
    @Transactional
    public void eliminar(UUID id) {
        WebDocumento documento = obtener(id);
        String archivo = documento.getRuta_archivo();
        documentoRepository.delete(documento);
        documentoRepository.flush();
        fileStorageService.delete(archivo);
    }

    @Override
    @Transactional
    public WebDocumentoResponse subirArchivo(UUID id, MultipartFile file) {
        WebDocumento documento = obtener(id);
        String referenciaAnterior = documento.getRuta_archivo();
        documento.setRuta_archivo(fileStorageService.store(file, TipoRecursoArchivo.WEB_DOCUMENTOS, id));
        documento.setNombre_original(nombreDescarga(file.getOriginalFilename()));
        documento.setTamano_bytes(file.getSize());
        WebDocumentoResponse response = guardar(documento);
        fileStorageService.delete(referenciaAnterior);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoDescarga obtenerArchivo(UUID id) {
        return archivo(obtener(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebDocumentoPublicoResponse> listarPublicados(String producto, TipoDocumentoWeb tipo) {
        String slugProducto = WebTextos.limpiar(producto);
        return documentoRepository.findPublicados().stream()
                .filter(d -> tipo == null || d.getTipo() == tipo)
                .filter(d -> slugProducto == null
                        || (d.getProducto() != null && d.getProducto().isPublicado()
                                && slugProducto.equals(d.getProducto().getSlug())))
                .map(documentoMapper::toPublicoResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoDescarga obtenerArchivoPublico(UUID id) {
        // Un documento no publicado responde igual que uno inexistente.
        WebDocumento documento = documentoRepository.findById(id)
                .filter(WebDocumento::isPublicado)
                .orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
        return archivo(documento);
    }

    private WebDocumento obtener(UUID id) {
        return documentoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NO_ENCONTRADO));
    }

    private ArchivoDescarga archivo(WebDocumento documento) {
        if (documento.getRuta_archivo() == null) {
            throw new ResourceNotFoundException("El documento no tiene archivo.");
        }
        return new ArchivoDescarga(documento.getRuta_archivo(), documento.getNombre_original());
    }

    private void setPublicado(WebDocumento documento, boolean publicado) {
        if (publicado && documento.getRuta_archivo() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sube el PDF del documento antes de publicarlo.");
        }
        documento.setPublicado(publicado);
    }

    private void aplicar(WebDocumento documento, WebDocumentoRequest request) {
        documento.setTitulo(WebTextos.limpiar(request.getTitulo()));
        documento.setDescripcion(WebTextos.limpiar(request.getDescripcion()));
        // Se reemplaza siempre (null = sin producto).
        documento.setProducto(request.getId_producto() == null ? null
                : productoRepository.findById(request.getId_producto())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado")));
    }

    /**
     * Nombre visible del PDF al descargarlo: sin rutas, comillas ni
     * caracteres de control (va en la cabecera Content-Disposition).
     */
    private String nombreDescarga(String original) {
        String nombre = original == null ? "" : original;
        nombre = nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1);
        nombre = nombre.replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (nombre.isEmpty() || nombre.equalsIgnoreCase(".pdf")) {
            nombre = "documento.pdf";
        }
        if (nombre.length() > LONGITUD_NOMBRE) {
            nombre = nombre.substring(nombre.length() - LONGITUD_NOMBRE);
        }
        return nombre;
    }

    // saveAndFlush: dispara @PreUpdate antes de mapear, así la respuesta trae
    // la fecha_actualizacion (y la versión de la URL del archivo) nueva.
    private WebDocumentoResponse guardar(WebDocumento documento) {
        documento.setUsuario_actualiza(usuarioActualProvider.obtener());
        return documentoMapper.toResponse(documentoRepository.saveAndFlush(documento));
    }
}
