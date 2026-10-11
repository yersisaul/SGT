package cfbd.co.sgt.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Alta/edición de una novedad. No incluye estado: toda novedad nace en
 * BORRADOR y se publica con PATCH /api/web/novedades/{id}/estado (permiso
 * web_novedad.publish). La portada se sube aparte (POST /{id}/imagen).
 * En la edición, destacado en null conserva su valor; id_categoria_novedad se
 * reemplaza siempre (null = sin categoría).
 */
@Getter
@Setter
public class WebNovedadRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede superar 200 caracteres")
    @Pattern(regexp = "(?s).*[\\p{L}\\p{N}].*", message = "El título debe tener letras o números")
    private String titulo;

    // Opcional: si no llega se genera desde el título; al editar, sin slug se conserva el actual.
    @Size(max = 120, message = "El slug no puede superar 120 caracteres")
    private String slug;

    @Size(max = 500, message = "El resumen no puede superar 500 caracteres")
    private String resumen;

    // Texto plano; párrafos separados por una línea en blanco.
    @Size(max = 50000, message = "El contenido no puede superar 50000 caracteres")
    private String contenido;

    private UUID id_categoria_novedad;

    private Boolean destacado;
}
