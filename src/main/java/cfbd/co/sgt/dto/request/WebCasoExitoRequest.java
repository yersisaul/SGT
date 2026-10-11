package cfbd.co.sgt.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Alta/edición de un caso de éxito. La imagen se sube aparte
 * (POST /api/web/casos-exito/{id}/imagen). En la edición, destacado, orden y
 * publicado en null conservan su valor actual; id_cliente_referencia e
 * id_producto se reemplazan siempre (null = sin vínculo).
 */
@Getter
@Setter
public class WebCasoExitoRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede superar 200 caracteres")
    @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*", message = "El título debe tener letras o números")
    private String titulo;

    // Opcional: si no llega se genera desde el título; al editar, sin slug se conserva el actual.
    @Size(max = 120, message = "El slug no puede superar 120 caracteres")
    private String slug;

    @Size(max = 500, message = "El resumen no puede superar 500 caracteres")
    private String resumen;

    @Size(max = 20000, message = "El contenido no puede superar 20000 caracteres")
    private String contenido;

    @Size(max = 500, message = "La URL externa no puede superar 500 caracteres")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "La URL externa debe empezar con http:// o https://")
    private String url_externa;

    // Formato yyyy-MM-dd.
    private LocalDate fecha_caso;

    @Size(max = 10, message = "Máximo 10 métricas")
    private List<@Valid MetricaCasoRequest> metricas;

    private UUID id_cliente_referencia;

    private UUID id_producto;

    private Boolean destacado;

    @Min(value = 0, message = "El orden no puede ser negativo")
    @Max(value = 9999, message = "El orden no puede superar 9999")
    private Integer orden;

    private Boolean publicado;
}
