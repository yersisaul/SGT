package cfbd.co.sgt.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import cfbd.co.sgt.dto.request.WebDocumentoRequest;
import cfbd.co.sgt.dto.response.WebDocumentoPublicoResponse;
import cfbd.co.sgt.dto.response.WebDocumentoResponse;
import cfbd.co.sgt.model.TipoDocumentoWeb;

public interface WebDocumentoService {

    /** Archivo a servir y nombre con el que se descarga. */
    record ArchivoDescarga(String referencia, String nombre) {
    }

    // ---- Panel de administración ----
    List<WebDocumentoResponse> listar();
    WebDocumentoResponse buscarPorId(UUID id);
    WebDocumentoResponse crear(WebDocumentoRequest request);
    WebDocumentoResponse editar(UUID id, WebDocumentoRequest request);
    /** 409 si se intenta publicar sin PDF. */
    WebDocumentoResponse cambiarPublicacion(UUID id, boolean publicado);
    void eliminar(UUID id);

    /** Sube o reemplaza el PDF y recalcula su tamaño. */
    WebDocumentoResponse subirArchivo(UUID id, MultipartFile file);
    ArchivoDescarga obtenerArchivo(UUID id);

    // ---- Sitio web público: publicados y con archivo ----
    /** Filtros opcionales: slug del producto y tipo. */
    List<WebDocumentoPublicoResponse> listarPublicados(String producto, TipoDocumentoWeb tipo);
    ArchivoDescarga obtenerArchivoPublico(UUID id);
}
