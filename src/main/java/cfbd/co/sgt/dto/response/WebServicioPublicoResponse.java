package cfbd.co.sgt.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** Servicio tal como lo ve el sitio web público: sin estado ni auditoría. */
@Getter
@Setter
public class WebServicioPublicoResponse {
    private String slug;
    private String nombre;
    private String descripcion_corta;
    private String descripcion;
    private String icono;
    private String url_img;
    private List<String> caracteristicas;
}
