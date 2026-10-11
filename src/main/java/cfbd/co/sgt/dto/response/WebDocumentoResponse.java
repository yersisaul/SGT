package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.UUID;

import cfbd.co.sgt.model.TipoDocumentoWeb;
import lombok.Getter;
import lombok.Setter;

/** Documento para el panel de administración (incluye no publicados y auditoría). */
@Getter
@Setter
public class WebDocumentoResponse {
    private UUID id_documento;
    private String titulo;
    private String descripcion;
    private TipoDocumentoWeb tipo;
    /** null mientras no se suba el PDF. */
    private String url_archivo;
    private String nombre_original;
    private Long tamano_bytes;
    private UUID id_producto;
    private String nombre_producto;
    private int orden;
    private boolean publicado;
    private Instant fecha_creacion;
    private Instant fecha_actualizacion;
    private String nombre_usuario_actualiza;
}
