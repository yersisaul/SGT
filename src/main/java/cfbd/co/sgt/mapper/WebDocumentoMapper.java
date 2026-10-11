package cfbd.co.sgt.mapper;

import org.springframework.stereotype.Component;

import cfbd.co.sgt.dto.response.WebCasoExitoPublicoResponse;
import cfbd.co.sgt.dto.response.WebDocumentoPublicoResponse;
import cfbd.co.sgt.dto.response.WebDocumentoResponse;
import cfbd.co.sgt.model.WebDocumento;
import cfbd.co.sgt.model.WebProducto;

@Component
public class WebDocumentoMapper {

    public WebDocumentoResponse toResponse(WebDocumento documento) {
        WebDocumentoResponse response = new WebDocumentoResponse();
        response.setId_documento(documento.getId_documento());
        response.setTitulo(documento.getTitulo());
        response.setDescripcion(documento.getDescripcion());
        response.setTipo(documento.getTipo());
        response.setUrl_archivo(documento.getRuta_archivo() != null
                ? WebMapperSoporte.archivoAdmin("documentos", documento.getId_documento(), "archivo",
                        documento.getFecha_actualizacion())
                : null);
        response.setNombre_original(documento.getNombre_original());
        response.setTamano_bytes(documento.getTamano_bytes());
        WebProducto producto = documento.getProducto();
        if (producto != null) {
            response.setId_producto(producto.getId_producto());
            response.setNombre_producto(producto.getNombre());
        }
        response.setOrden(documento.getOrden());
        response.setPublicado(documento.isPublicado());
        response.setFecha_creacion(documento.getFecha_creacion());
        response.setFecha_actualizacion(documento.getFecha_actualizacion());
        response.setNombre_usuario_actualiza(WebMapperSoporte.nombreCompleto(documento.getUsuario_actualiza()));
        return response;
    }

    /** Solo para documentos publicados y con archivo (ver WebDocumentoRepository.findPublicados). */
    public WebDocumentoPublicoResponse toPublicoResponse(WebDocumento documento) {
        WebDocumentoPublicoResponse response = new WebDocumentoPublicoResponse();
        response.setTitulo(documento.getTitulo());
        response.setDescripcion(documento.getDescripcion());
        response.setTipo(documento.getTipo());
        response.setUrl_archivo(WebMapperSoporte.imagenPublica("documentos", documento.getId_documento(),
                documento.getFecha_actualizacion()));
        response.setNombre_original(documento.getNombre_original());
        response.setTamano_bytes(documento.getTamano_bytes() != null ? documento.getTamano_bytes() : 0);
        WebProducto producto = documento.getProducto();
        if (producto != null && producto.isPublicado()) {
            response.setProducto(new WebCasoExitoPublicoResponse.ProductoEnlace(producto.getSlug(), producto.getNombre()));
        }
        return response;
    }
}
