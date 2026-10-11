package cfbd.co.sgt.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Alta/edición de un servicio del sitio web. La imagen se sube aparte
 * (POST /api/web/servicios/{id}/imagen). En la edición, orden y publicado en
 * null conservan su valor actual.
 */
@Getter
@Setter
public class WebServicioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*", message = "El nombre debe tener letras o números")
    private String nombre;

    // Opcional: si no llega se genera desde el nombre; al editar, sin slug se conserva el actual.
    @Size(max = 120, message = "El slug no puede superar 120 caracteres")
    private String slug;

    @Size(max = 300, message = "La descripción corta no puede superar 300 caracteres")
    private String descripcion_corta;

    @Size(max = 20000, message = "La descripción no puede superar 20000 caracteres")
    private String descripcion;

    // Nombre de ícono de Font Awesome 6 sin prefijo (p. ej. "server", "shield-halved").
    @Size(max = 60, message = "El ícono no puede superar 60 caracteres")
    @Pattern(regexp = "^$|^[a-z0-9-]+$", message = "El ícono solo admite minúsculas, números y guiones")
    private String icono;

    @Size(max = 30, message = "Máximo 30 características")
    private List<@NotBlank(message = "Una característica no puede estar vacía")
            @Size(max = 300, message = "Cada característica admite hasta 300 caracteres") String> caracteristicas;

    @Min(value = 0, message = "El orden no puede ser negativo")
    @Max(value = 9999, message = "El orden no puede superar 9999")
    private Integer orden;

    private Boolean publicado;
}
