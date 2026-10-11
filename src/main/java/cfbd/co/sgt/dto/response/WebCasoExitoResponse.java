package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import cfbd.co.sgt.model.MetricaCaso;
import lombok.Getter;
import lombok.Setter;

/** Caso de éxito para el panel de administración (incluye no publicados y auditoría). */
@Getter
@Setter
public class WebCasoExitoResponse {
    private UUID id_caso_exito;
    private String slug;
    private String titulo;
    private String resumen;
    private String contenido;
    private String url_img;
    private String url_externa;
    private LocalDate fecha_caso;
    private List<MetricaCaso> metricas;
    private UUID id_cliente_referencia;
    private String nombre_cliente;
    private UUID id_producto;
    private String nombre_producto;
    private boolean destacado;
    private int orden;
    private boolean publicado;
    private Instant fecha_creacion;
    private Instant fecha_actualizacion;
    private String nombre_usuario_actualiza;
}
