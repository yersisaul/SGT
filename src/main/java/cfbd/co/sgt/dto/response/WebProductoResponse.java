package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import cfbd.co.sgt.model.ModuloProducto;
import lombok.Getter;
import lombok.Setter;

/** Producto para el panel de administración (incluye no publicados y auditoría). */
@Getter
@Setter
public class WebProductoResponse {
    private UUID id_producto;
    private String slug;
    private String nombre;
    private String subtitulo;
    private String descripcion_corta;
    private String descripcion;
    private String url_img;
    private String url_video;
    private List<String> caracteristicas;
    private List<ModuloProducto> modulos;
    private boolean destacado;
    private int orden;
    private boolean publicado;
    private Instant fecha_creacion;
    private Instant fecha_actualizacion;
    private String nombre_usuario_actualiza;
}
