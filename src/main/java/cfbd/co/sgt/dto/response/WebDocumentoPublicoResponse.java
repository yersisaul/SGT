package cfbd.co.sgt.dto.response;

import cfbd.co.sgt.model.TipoDocumentoWeb;
import lombok.Getter;
import lombok.Setter;

/**
 * Documento tal como lo ve el sitio web público. producto solo llega si
 * también está publicado (si no, null).
 */
@Getter
@Setter
public class WebDocumentoPublicoResponse {
    private String titulo;
    private String descripcion;
    private TipoDocumentoWeb tipo;
    private String url_archivo;
    private String nombre_original;
    /** El sitio lo muestra legible (p. ej. "9,1 MB"). */
    private long tamano_bytes;
    private WebCasoExitoPublicoResponse.ProductoEnlace producto;
}
