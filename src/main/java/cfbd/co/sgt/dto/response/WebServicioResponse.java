package cfbd.co.sgt.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** Servicio para el panel de administración (incluye no publicados y auditoría). */
@Getter
@Setter
public class WebServicioResponse {
    private UUID id_servicio;
    private String slug;
    private String nombre;
    private String descripcion_corta;
    private String descripcion;
    private String icono;
    private String url_img;
    private List<String> caracteristicas;
    private int orden;
    private boolean publicado;
    private Instant fecha_creacion;
    private Instant fecha_actualizacion;
    private String nombre_usuario_actualiza;
}
