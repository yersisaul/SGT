package cfbd.co.sgt.dto.request;

import java.util.UUID;

import cfbd.co.sgt.model.TipoDocumentoWeb;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Alta/edición de un documento del sitio web. El PDF se sube aparte
 * (POST /api/web/documentos/{id}/archivo); sin archivo no se puede publicar.
 * En la edición, tipo, orden y publicado en null conservan su valor;
 * id_producto se reemplaza siempre (null = sin producto). En el alta, tipo
 * por defecto BROCHURE.
 */
@Getter
@Setter
public class WebDocumentoRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede superar 200 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
    private String descripcion;

    private TipoDocumentoWeb tipo;

    private UUID id_producto;

    @Min(value = 0, message = "El orden no puede ser negativo")
    @Max(value = 9999, message = "El orden no puede superar 9999")
    private Integer orden;

    private Boolean publicado;
}
