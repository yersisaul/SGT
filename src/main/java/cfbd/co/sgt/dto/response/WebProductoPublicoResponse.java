package cfbd.co.sgt.dto.response;

import java.util.List;

import cfbd.co.sgt.model.ModuloProducto;
import lombok.Getter;
import lombok.Setter;

/** Producto tal como lo ve el sitio web público: sin estado ni auditoría. */
@Getter
@Setter
public class WebProductoPublicoResponse {
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
}
