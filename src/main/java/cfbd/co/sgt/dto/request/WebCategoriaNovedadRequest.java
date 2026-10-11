package cfbd.co.sgt.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Alta/edición de una categoría de novedades. En la edición, orden en null conserva su valor. */
@Getter
@Setter
public class WebCategoriaNovedadRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*", message = "El nombre debe tener letras o números")
    private String nombre;

    // Opcional: si no llega se genera desde el nombre; al editar, sin slug se conserva el actual.
    @Size(max = 120, message = "El slug no puede superar 120 caracteres")
    private String slug;

    @Min(value = 0, message = "El orden no puede ser negativo")
    @Max(value = 9999, message = "El orden no puede superar 9999")
    private Integer orden;
}
