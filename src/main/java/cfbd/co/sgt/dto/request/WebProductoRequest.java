package cfbd.co.sgt.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Alta/edición de un producto del sitio web. La imagen se sube aparte
 * (POST /api/web/productos/{id}/imagen). En la edición, destacado, orden y
 * publicado en null conservan su valor actual.
 */
@Getter
@Setter
public class WebProductoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*", message = "El nombre debe tener letras o números")
    private String nombre;

    // Opcional: si no llega se genera desde el nombre; al editar, sin slug se conserva el actual.
    @Size(max = 120, message = "El slug no puede superar 120 caracteres")
    private String slug;

    @Size(max = 200, message = "El subtítulo no puede superar 200 caracteres")
    private String subtitulo;

    @Size(max = 300, message = "La descripción corta no puede superar 300 caracteres")
    private String descripcion_corta;

    @Size(max = 20000, message = "La descripción no puede superar 20000 caracteres")
    private String descripcion;

    @Size(max = 500, message = "La URL del video no puede superar 500 caracteres")
    @Pattern(regexp = "^$|^https://\\S+$", message = "La URL del video debe empezar con https://")
    private String url_video;

    @Size(max = 30, message = "Máximo 30 características")
    private List<@NotBlank(message = "Una característica no puede estar vacía")
            @Size(max = 300, message = "Cada característica admite hasta 300 caracteres") String> caracteristicas;

    @Size(max = 60, message = "Máximo 60 módulos")
    private List<@Valid ModuloProductoRequest> modulos;

    private Boolean destacado;

    @Min(value = 0, message = "El orden no puede ser negativo")
    @Max(value = 9999, message = "El orden no puede superar 9999")
    private Integer orden;

    private Boolean publicado;
}
