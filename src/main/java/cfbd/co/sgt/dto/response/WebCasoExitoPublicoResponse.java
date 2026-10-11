package cfbd.co.sgt.dto.response;

import java.time.LocalDate;
import java.util.List;

import cfbd.co.sgt.model.MetricaCaso;
import lombok.Getter;
import lombok.Setter;

/**
 * Caso de éxito tal como lo ve el sitio web público. cliente y producto solo
 * llegan si también están publicados (si no, null).
 */
@Getter
@Setter
public class WebCasoExitoPublicoResponse {
    private String slug;
    private String titulo;
    private String resumen;
    private String contenido;
    private String url_img;
    private String url_externa;
    private LocalDate fecha_caso;
    private List<MetricaCaso> metricas;
    private WebClienteReferenciaPublicoResponse cliente;
    private ProductoEnlace producto;
    private boolean destacado;

    /** Lo necesario para enlazar a /productos/{slug} desde el caso. */
    public record ProductoEnlace(String slug, String nombre) {
    }
}
