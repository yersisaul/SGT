package cfbd.co.sgt.dto.response;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * Novedad tal como la ve el sitio web público. En el listado, contenido llega
 * en null (se pide el detalle por slug); categoria es null si no tiene.
 */
@Getter
@Setter
public class WebNovedadPublicoResponse {
    private String slug;
    private String titulo;
    private String resumen;
    private String contenido;
    private String url_portada;
    private Instant fecha_publicacion;
    private CategoriaEnlace categoria;
    private String autor;
    private boolean destacado;

    /** Lo necesario para filtrar con ?categoria={slug}. */
    public record CategoriaEnlace(String slug, String nombre) {
    }
}
