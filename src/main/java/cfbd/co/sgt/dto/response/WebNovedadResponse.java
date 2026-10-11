package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.UUID;

import cfbd.co.sgt.model.EstadoNovedad;
import lombok.Getter;
import lombok.Setter;

/** Novedad para el panel de administración. */
@Getter
@Setter
public class WebNovedadResponse {
    private UUID id_novedad;
    private String slug;
    private String titulo;
    private String resumen;
    private String contenido;
    private String url_portada;
    private EstadoNovedad estado;
    private Instant fecha_publicacion;
    /** PUBLICADO con fecha ya cumplida; PUBLICADO con fecha futura = programada. */
    private boolean visible_en_sitio;
    private UUID id_categoria_novedad;
    private String nombre_categoria;
    private String nombre_autor;
    private boolean destacado;
    private Instant fecha_creacion;
    private Instant fecha_actualizacion;
    private String nombre_usuario_actualiza;
}
